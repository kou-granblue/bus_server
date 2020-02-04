package Gtfs_Servlet;

import java.io.IOException;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.util.Date;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.tools.ant.taskdefs.Recorder.VerbosityLevelChoices;
import org.onebusway.gtfs_realtime.exporter.GtfsRealtimeLibrary;

import com.google.protobuf.Message;
import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedEntity.Builder;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.Position;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.VehicleDescriptor;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition;
import com.ryosuke.gtfs_realtime.FeedMessageProvider;

import acount.Pdo;
import utils.MySingleton;

/**
 * sessionのidにprovider
 * sessionのnameがユーザーのディレクトリ
 */

/**
 * Servlet implementation class AddVehiclePosition
 */
@WebServlet("/AddVehiclePosition")
public class AddVehiclePosition extends AbstractAddServlet {
	private static final long serialVersionUID = 1L;
	private String path;
	private static String FILENAME = "vehicle-positions.dat";

	private static String USERDATA_DIRECTORY = "UserData Directory Path";
	private static String FINISH ="finish";
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request,response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
		HttpSession session = request.getSession(false);
		response.setCharacterEncoding("UTF-8");
		System.out.println("characterencode:"+response.getCharacterEncoding());
		System.out.println("session:"+session);
		System.out.println(session.getAttribute("login"));
		if(session.getAttribute("login") == null ){
			response.getWriter().println("Time out");
			return;
		}

		try {
			Pdo.connectionCheck();
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException | SQLException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
        URLCodec codec = new URLCodec("UTF-8");
        URLDecoder _decoder = new URLDecoder();
		// TODO Auto-generated method stub
		super.doPost(request, response);
		String userdirectory = USERDATA_DIRECTORY+session.getAttribute("userfoldername");
		FeedMessageProvider provider = (FeedMessageProvider)_context.getAttribute((String)session.getAttribute("userid"));
		Message message;
		if(request.getParameter("debug") != null){
			response.getWriter().print(getMessage((FeedMessageProvider)_context.getAttribute((String)session.getAttribute("userid"))));
			return;
		}
		String trainNumber = null;
		trainNumber = _decoder.decode(new String(request.getParameter("trainNumber").getBytes("ISO-8859-1")), "UTF-8");
		String userid = (String)session.getAttribute("userid");
		String requestMessage = request.getParameter("message");

		//tripi終了通知
		if(trainNumber != null && requestMessage !=null && requestMessage.equals(FINISH)){
			deleteFeedMessage(trainNumber,userid);
			writeMessageToFile(userdirectory,provider);
			return;
		}
		double lat = Double.parseDouble(request.getParameter("latitude"));
		double lon = Double.parseDouble(request.getParameter("longitude"));
		String trip = request.getParameter("trip");

		int nextStopSequence =-1;
		if(request.getParameter("nextStopSequence")!=null) {
			nextStopSequence = Integer.parseInt(request.getParameter("nextStopSequence"));
		}else {
			nextStopSequence = -1;
		}
		if(request.getParameter("debug") != null){
			response.getWriter().println("trainNumber:"+trainNumber+" lat:"+lat+" lon:"+lon);
		}
		buildFeedMessage(trainNumber,lat, lon, trip,session,nextStopSequence);
		writeMessageToFile(userdirectory,provider);
		int user_id = Integer.parseInt(userid);
		int trip_id = Integer.parseInt(trip);
		insertLog(user_id, trip_id, trainNumber, lat, lon);
	}

	@Override
	public void init(ServletConfig config) throws ServletException{
		System.out.println("init in servlet");
		super.init(config);
	}

	@Override
	protected Message getMessage(FeedMessageProvider provider) {
		// TODO 自動生成されたメソッド・スタブ
		return provider.getVehiclePositions();
	}

	@Override
	protected String getFileName(){
		return FILENAME;
	}

	/*
	 * FeedMessageの作成と登録
	 */
//	@Override
	private void buildFeedMessage(String trainNumber,double lat,double lon,String trip,HttpSession session, int stopSequence) {
		// TODO 自動生成されたメソッド・スタブ
		System.out.println("start buildFeedMessage");
		Builder newentity = getFeedEntity(trainNumber,lat,lon,trip,stopSequence);
		FeedMessage.Builder vehiclePositions = GtfsRealtimeLibrary.createFeedMessageBuilder();

		// sessionからuseridを取得
		FeedMessageProvider provider = (FeedMessageProvider)_context.getAttribute((String)session.getAttribute("userid"));
		FeedMessage preVehiclePositions =(FeedMessage)getMessage(provider);
		if(preVehiclePositions.getEntityCount()==0){
			vehiclePositions.addEntity(newentity);
			provider.setVehiclePositions(vehiclePositions.build());
			return;
		}

		/*entityを追加したことを確認するフラッグ：追加したらtrue*/
		boolean flag = false;
		for (FeedEntity entity : preVehiclePositions.getEntityList()) {
			/*
			 * vehiclepositionの有無
			if (!entity.hasVehicle()) {
			    continue;
			  }
			VehiclePosition vehicle = entity.getVehicle();
			*/
			/*vehicledescriptorの有無
			if(!vehicle.hasVehicle()){
				continue;
			}
			VehicleDescriptor vehicledescriptor = vehicle.getVehicle();*/
			if(!entity.hasId()){
				continue;
			}
			if(trainNumber.equals(entity.getId())){
				vehiclePositions.addEntity(newentity);
				flag=true;
			}else{
			vehiclePositions.addEntity(entity);
			}
	    }
		if(!flag){
			vehiclePositions.addEntity(newentity);
		}
		provider.setVehiclePositions(vehiclePositions.build());
	}



	/**
	 * 送られてきたデータからエンティティの作成をする
	 * @param trainNumber
	 * @param lat
	 * @param lon
	 * @param route
	 * @return
	 */
	private Builder getFeedEntity(String trainNumber,double lat,double lon,String trip, int stopSequence){
//**************ここから
		TripDescriptor.Builder tripDescriptor = TripDescriptor.newBuilder();

	    tripDescriptor.setTripId(trip);

	    VehicleDescriptor.Builder vehicleDescriptor = VehicleDescriptor.newBuilder();
	    vehicleDescriptor.setId(trainNumber);
		Position.Builder position = Position.newBuilder();
	    position.setLatitude((float) lat);
	    position.setLongitude((float) lon);

	    VehiclePosition.Builder vehiclePosition = VehiclePosition.newBuilder();
	    vehiclePosition.setPosition(position);
	    vehiclePosition.setTrip(tripDescriptor);
	    vehiclePosition.setVehicle(vehicleDescriptor);
	    if(stopSequence >= 0){
	    vehiclePosition.setCurrentStopSequence(stopSequence);
	    }
		FeedEntity.Builder vehiclePositionEntity = FeedEntity.newBuilder();
	    vehiclePositionEntity.setId(trainNumber);
	    vehiclePositionEntity.setVehicle(vehiclePosition);

	    System.out.println("finish entity");
	    return vehiclePositionEntity;
//******************ここ繰り返す

	}

	/**
	 * ログを記録する
	 * @param user_id
	 * @param trip_id
	 * @param trainNumber
	 * @param lat
	 * @param lon
	 */
	public void insertLog(int user_id,int trip_id, String trainNumber, double lat, double lon){
		System.out.println("at vechicleposition insertlog");
		int vehicle_id = Pdo.getVehicleID(user_id, trainNumber);
		int vehiclePosition_id = Pdo.checkVehiclePositionLog(user_id, trip_id, vehicle_id);
		if(vehiclePosition_id == -1){
			System.out.println("at vehicleposition : not trip now");
			System.out.println("user_id = "+user_id+"trip_id = "+trip_id+"vehicle_id = "+vehicle_id+"starttime_id = "+Pdo.getTripStartTime(user_id, trip_id));
			vehiclePosition_id = Pdo.insertVehiclePositionLog(user_id, trip_id, vehicle_id, Pdo.getTripStartTime(user_id, trip_id));
		}
		if(vehiclePosition_id != -1){
			Pdo.insertVehiclePositionDetailesLog(vehiclePosition_id, lat, lon);
		}
	}

	/**
	 * useridのvehicleidを持つentity削除
	 * @param vehicleid
	 * @param userid
	 */
	private void deleteFeedMessage(String vehicleid,String userid){
		FeedMessage.Builder vehiclePositions = GtfsRealtimeLibrary.createFeedMessageBuilder();
		FeedMessageProvider provider = (FeedMessageProvider)_context.getAttribute(userid);
		FeedMessage preVehiclePositions =(FeedMessage)getMessage(provider);
		for (FeedEntity entity : preVehiclePositions.getEntityList()) {
			if(!entity.hasId()){
				continue;
			}
			if(vehicleid.equals(entity.getId())){
			}else{
			vehiclePositions.addEntity(entity);
			}
		}
		provider.setVehiclePositions(vehiclePositions.build());
	}

}

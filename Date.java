import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.onebusway.gtfs_realtime.exporter.GtfsRealtimeLibrary;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.ryosuke.gtfs_realtime.FeedMessageProvider;

import announce.AnnounceUpdateList;
import utils.MySingleton;

@WebServlet(name = "date", urlPatterns = { "/date" })
public class Date extends HttpServlet{

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		Date date = new Date();
		Calendar c = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 E曜日 HH:mm:ss");
		response.setContentType("text/html; charset=utf-8");
		response.getWriter().append("Served at: ").append(sdf.format(c.getTime()));
		response.getWriter().println("wawawa");
	}


	  private static int STRETCH_COUNT = 1000;

	  
	  public static String getSaltedPassword(String password, String userId) {
	    String salt = getSha256(userId);
	    return getSha256(salt + password);
	  }

	  public static String getStretchedPassword(String password, String userId) {
	    String salt = getSha256(userId);
	    String hash = "";

	    for (int i = 0; i < STRETCH_COUNT; i++) {
	      hash = getSha256(hash + salt + password);
	    }

	    return hash;
	  }

	  private static String getSha256(String target) {
	    MessageDigest md = null;
	    StringBuffer buf = new StringBuffer();
	    try {
	      md = MessageDigest.getInstance("SHA-256");
	      md.update(target.getBytes());
	      byte[] digest = md.digest();

	      for (int i = 0; i < digest.length; i++) {
	        buf.append(String.format("%02x", digest[i]));
	      }

	    } catch (NoSuchAlgorithmException e) {
	      e.printStackTrace();
	    }

	    return buf.toString();
	  }
}

package acount;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.security.crypto.bcrypt.BCrypt;

import utils.MySingleton;

@WebServlet("/LoginCheck")
public class LoginCheck extends HttpServlet {

    protected Connection conn = null;

    public void init() throws ServletException{

        System.out.println("initialize...");

		try {
			conn = Pdo.getConnection();
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException | SQLException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
    }

    @Override
    public void destroy(){
    	System.out.println("destroy...");
    	super.destroy();
    	System.out.println("destroy2...");
    	try {
			conn.close();
		} catch (SQLException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
    }


    public void doGet(HttpServletRequest request, HttpServletResponse response)
    		throws IOException, ServletException{
    	doPost(request,response);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException{
        response.setContentType("text/html; charset=utf-8");
        PrintWriter out = response.getWriter();

        Boolean valid = null;

		try {
			conn = Pdo.connectionCheck();
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException | SQLException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		try {
			valid = conn.isValid(MySingleton.getDbTimeout());
		} catch (SQLException e1) {
			// TODO 自動生成された catch ブロック
			e1.printStackTrace();
		}

        System.out.println("LoginCheck test");
        Map<String,String[]> userdata = request.getParameterMap();

        String user = userdata.get("username")[0];
        String pass = userdata.get("password")[0];
        System.out.println("LoginCheck test2");
        HttpSession session = request.getSession(true);

      
//        pass = format(pass);
        System.out.println("start auth:"+user);
        String userfoldername = authUser(user, pass, session);
        System.out.println("auth completed:"+userfoldername);
        if (userfoldername != null){
            /* 認証済みにセット */
            session.setAttribute("login", "OK");

            /* 認証成功後は必ずMonthViewサーブレットを呼びだす */
//            response.sendRedirect("/schedule/MonthView");
            out.print(userfoldername);
        }else{
            /* 認証に失敗したら、ログイン画面に戻す */
            session.setAttribute("status", "Not Auth");
//            response.sendRedirect("/schedule/LoginPage");
            out.print("login miss");
        }
    }

    protected String authUser(String user, String pass, HttpSession session){
        if (user == null || user.length() == 0 || pass == null || pass.length() == 0){
            return null;
        }
        int retryCount = 3;
        do{
        try {

            String sql = "SELECT * FROM userdata WHERE user = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);

            pstmt.setString(1, user);;
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()){
            	int userid = rs.getInt("id");
                String username = rs.getString("user");
                String password = rs.getString("pass");
                if(BCrypt.checkpw(pass, format(password))){
          

                File filepath = new File(MySingleton.getInstance().getUserDataDirectory()+userid);
                filepath.mkdir();
                session.setAttribute("userid", Integer.toString(userid));
                session.setAttribute("username", username);

                session.setAttribute("userfoldername",userid);
                return String.valueOf(userid);
                }else{
                	return null;
                }
            }else{
                return null;
            }
        }catch (SQLException e){
        	e.printStackTrace();
        	String sqlState = e.getSQLState();
        	System.out.println("at LoginCheck error:"+sqlState);
        	retryCount--;
        }
        }while(retryCount>0);
        return null;
    }

    protected String format(String str){
    	String result ="";
    	String substr = str.substring(0,7);
    	result = str.replace(substr, "$2a$10$");
    	return result;
    }
}

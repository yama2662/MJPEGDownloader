package com.mpegdownloader;



//import gnu.io.*;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.Calendar;

public class MJPEGDownloader {

	public static void main(String[] args) {

		boolean getFlag = true;
		try{
			//シリアルポート確保============================-
			//使用ポート確保 arduino接続時に確認
			CommPortIdentifier comID = CommPortIdentifier.getPortIdentifier("COM4");

			//port open
			CommPort commPort= comID.open("hoge",2000);

			//シリアルポートのインスタンス生成？
			SerialPort port =(SerialPort)commPort;


			//シリアルポートの設定
			port.setSerialPortParams(9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
			port.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);

			OutputStream out = port.getOutputStream();




			Calendar calender = Calendar.getInstance();

			int second;
			int pic_num = 1;

			second =0;

			while(true){

				calender = Calendar.getInstance();
				second = calender.get(Calendar.SECOND);

				if(second==58)
					out.write(1);

				if(second==0&&getFlag==true){
					pic_num = get_data(pic_num);
					getFlag=false;
				}
				if(second==1){
					out.write(0);
					getFlag=true;
				}

			}
		}catch( Exception e ){
			System.out.println("ERROR発生:" + e);
		}
	}

	public static int get_data(int pic_num) {
		HttpURLConnection connection = null;
		URL url = null;

		byte[] responseBuffer = new byte[60000];
		byte[] imageBuffer = new byte[60000];

		try {
			// Basic認証
			Authenticator.setDefault(new HTTPAuthenticator("admin", "admin"));

			url = new URL("http://192.168.0.51:7777/media/?action=stream");
			connection = (HttpURLConnection) url.openConnection();
			connection.connect();

			int responseCode = connection.getResponseCode();
			if (responseCode == 200) {
				BufferedInputStream bufInput = new BufferedInputStream(
						connection.getInputStream());

				for (int i = 0; i < responseBuffer.length; i++) {
					responseBuffer[i] = (byte) (bufInput.read() & (byte) 0xff);
				}

				bufInput.close();

				boolean startFlag = false;
				int imageBufferCount = 0;
				for (int i = 0; i < responseBuffer.length; i++) {
					if (responseBuffer[i] == (byte) 0xff) {
						if (responseBuffer[i + 1] == (byte) 0xd8) {
							startFlag = true;
							System.out.println("Start.");
						}
					}

					if (startFlag == true) {
						imageBuffer[imageBufferCount++] = responseBuffer[i];
						if (responseBuffer[i] == (byte) 0xff) {
							if (responseBuffer[i + 1] == (byte) 0xd9) {
								imageBuffer[imageBufferCount++] = responseBuffer[i + 1];
								System.out.println("End.");
								break;
							}
						}
					}
				}

				FileOutputStream fileOutput = new FileOutputStream("sample"
						+ pic_num + ".jpg");
				fileOutput.write(imageBuffer, 0, imageBufferCount);

				fileOutput.close();

				System.out.println("Done.");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
		return ++pic_num;

	}

	static class HTTPAuthenticator extends Authenticator {
		private String username, password;

		public HTTPAuthenticator(String user, String pass) {
			username = user;
			password = pass;
		}

		protected PasswordAuthentication getPasswordAuthentication() {
			System.out.println("Requesting Host  : " + getRequestingHost());
			System.out.println("Requesting Port  : " + getRequestingPort());
			System.out.println("Requesting Prompt : " + getRequestingPrompt());
			System.out.println("Requesting Protocol: "
					+ getRequestingProtocol());
			System.out.println("Requesting Scheme : " + getRequestingScheme());
			System.out.println("Requesting Site  : " + getRequestingSite());
			return new PasswordAuthentication(username, password.toCharArray());
		}
	}
}

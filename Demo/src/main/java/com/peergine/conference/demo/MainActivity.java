package com.peergine.conference.demo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.peergine.android.conference.pgLibConference;
import com.peergine.android.conference.pgVideoPutMode;
import com.peergine.plugin.lib.pgLibJNINode;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends Activity implements CompoundButton.OnCheckedChangeListener {

	private int m_iMode = 0;
	private String m_sGroup = "";
	private String m_sChair = "";
	private String m_sUser = "";
	private String m_sPass = "";
	//private String sSvr ="120.55.85.29:7781";
	private String sSvr ="connect.peergine.com:7781";
	private String m_sMemb = "";

	private static pgLibConference m_Conf = new pgLibConference();

	private int RIDLaout[]={R.id.layoutVideoS0,R.id.layoutVideoS1,R.id.layoutVideoS2,R.id.layoutVideoS3};
	EditText m_editText_name=null;
	CheckBox m_CheckBox=null;
	EditText m_editText_User = null;

	private Button m_btnStart = null;
	private Button m_btnClean = null;

	private EditText m_editText_Notify =null;
	private Button m_btnNotifySend= null;


	private boolean bChair = false;
	private String m_sVideoParam=
			"(Code){3}(Mode){2}(FrmRate){40}" +
			"(LCode){3}(LMode){3}(LFrmRate){30}" +
			"(Portrait){0}(Rotate){1}(BitRate){400}(CameraNo){"+ Camera.CameraInfo.CAMERA_FACING_FRONT+"}"+
			"(AudioSpeechDisable){3}";
	private Button m_btntest=null;

	class PG_MEMB{
		String sPeer="";
		Boolean bOpen=false;
		SurfaceView pView=null;
		LinearLayout pLayout=null;
	}

	private static ArrayList<PG_MEMB> memberArray = new ArrayList<>();
	public static void setCameraDisplayOrientation (Activity activity, int cameraId, android.hardware.Camera camera) {
		android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
		android.hardware.Camera.getCameraInfo (cameraId , info);
		int rotation = activity.getWindowManager ().getDefaultDisplay ().getRotation ();
		int degrees = 0;
		switch (rotation) {
			case Surface.ROTATION_0:
				degrees = 0;
				break;
			case Surface.ROTATION_90:
				degrees = 90;
				break;
			case Surface.ROTATION_180:
				degrees = 180;
				break;
			case Surface.ROTATION_270:
				degrees = 270;
				break;
		}
		int result;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			result = (info.orientation + degrees) % 360;
			result = (360 - result) % 360;   // compensate the mirror
		} else {
			// back-facing
			result = ( info.orientation - degrees + 360) % 360;
		}
		camera.setDisplayOrientation (result);
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//		Camera camera=null;
//		setCameraDisplayOrientation(this,Camera.CameraInfo.CAMERA_FACING_FRONT,camera);
		for(int i=0;i<4;i++)
		{
				PG_MEMB oMemb = new PG_MEMB();
				oMemb.pLayout = (LinearLayout) findViewById(RIDLaout[i]);
				memberArray.add(oMemb);
		}

		m_editText_name=(EditText)findViewById(R.id.editText_name);
		m_editText_User = (EditText) findViewById(R.id.editText_user);
		m_CheckBox = (CheckBox)findViewById(R.id.checkBox);
		m_CheckBox.setOnCheckedChangeListener(this);
		m_btnStart = (android.widget.Button) findViewById(R.id.btn_Start);
		m_btnStart.setOnClickListener(m_OnClink);


		m_btnClean =(android.widget.Button) findViewById(R.id.btn_Clean);
		m_btnClean.setOnClickListener(m_OnClink);

		m_editText_Notify =(EditText)findViewById(R.id.editText_notify);

		m_btnNotifySend=(android.widget.Button) findViewById(R.id.btn_notifysend);
		m_btnNotifySend.setOnClickListener(m_OnClink);

		m_btntest=(android.widget.Button) findViewById(R.id.button);
		m_btntest.setOnClickListener(m_OnClink);
		m_Conf.SetEventListener(m_OnEvent);



		//m_listMember.setAdapter(new ArrayAdapter<String>(this, R.id.linearLayoutMain, data));
	}
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			Log.d("pgLiveCapture", "onKeyDown, KEYCODE_BACK");
			ExitDialog();
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}

	private DialogInterface.OnClickListener m_DlgClick = new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int which) {
			if (which == AlertDialog.BUTTON_POSITIVE) {
				m_Conf.Clean();
				android.os.Process.killProcess(android.os.Process.myPid());
			}
		}
	};

	public void ExitDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Confirm");
		builder.setMessage("Are you sure to exit?");
		builder.setPositiveButton("YES", m_DlgClick);
		builder.setNegativeButton("NO", m_DlgClick);
		builder.show();
	}
	private int m_iAngle=0;
	public void SetRotate(int iAngle) {
		pgLibJNINode Node = m_Conf.GetNode();
		if (Node != null) {
			if (Node.ObjectAdd("_vTemp", "PG_CLASS_Video", "", 0)) {
				Node.ObjectRequest("_vTemp", 2, "(Item){2}(Value){" + iAngle + "}", "");
				Node.ObjectDelete("_vTemp");
			}
		}
	}


	//屏幕旋转 设置摄像头旋转角度	@Override
	public void onConfigurationChanged(Configuration newConfig) {
			 super.onConfigurationChanged(newConfig);
			// 检测屏幕的方向：纵向或横向

			if (this.getResources().getConfiguration().orientation
				== Configuration.ORIENTATION_LANDSCAPE) {
				//当前为横屏， 在此处添加额外的处理代码
				SetRotate(0);
				// todo 或者添加 为180度
			}
			else if (this.getResources().getConfiguration().orientation
				== Configuration.ORIENTATION_PORTRAIT) {
				//当前为竖屏， 在此处添加额外的处理代码
				SetRotate(90);
			}
		}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		//清理会议模块，先关闭视频 再关闭预览 然后Clean
		for(int i=3;i>=0;i--)
		{
			PG_MEMB oMemb=memberArray.get(i);
			if(!oMemb.sPeer.equals(""))
			{
				m_Conf.VideoClose(oMemb.sPeer);
			}
		}

		m_Conf.PreviewDestroy();
		m_Conf.Clean();
	}

	/// Sdk扩展运用之添加通信节点，  使用之后会产生PeerSync事件
	// 添加节点连接
	// sPeer: 对端的节点名（用户名）
	public boolean PeerAdd(String sPeer) {
		if (sPeer.equals("")) {
			return false;
		}

		String sPeerTemp = sPeer;
		if (sPeerTemp.indexOf("_DEV_") != 0) {
			sPeerTemp = ("_DEV_" + sPeer);
		}

		pgLibJNINode Node = m_Conf.GetNode();
		if (Node == null) {
			return false;
		}

		String sClass = Node.ObjectGetClass(sPeerTemp);
		if (sClass.equals("PG_CLASS_Peer")) {
			return true;
		}

		if (!sClass.equals("")) {
			Node.ObjectDelete(sPeerTemp);
		}

		return Node.ObjectAdd(sPeerTemp, "PG_CLASS_Peer", "", 0x10000);
	}


	// 设置设备的麦克风的采样率。
	// 在初始化成功之后，打开音频通话之前调用。
	// iRate: 采样率，单位HZ/秒。有效值：8000, 1600, 32000
	//
	public void SetAudioSampleRate(int iRate) {
		pgLibJNINode Node = m_Conf.GetNode();
		if (Node != null) {
			// Set microphone sample rate
			if (Node.ObjectAdd("_AudioTemp", "PG_CLASS_Audio", "", 0)) {
				Node.ObjectRequest("_AudioTemp", 2, "(Item){2}(Value){" + iRate + "}", "");
				Node.ObjectDelete("_AudioTemp");
			}
		}
	}

	///// Sdk扩展运用之添加通信节点，  使用之后会产生PeerSync事件
	// 删除节点连接。（一般不用主动删除节点，因为如果没有通信，节点连接会自动老化。）
	// sPeer: 对端的节点名（用户名）
	public void PeerDelete(String sPeer) {
		if (sPeer.equals("")) {
			return;
		}

		String sPeerTemp = sPeer;
		if (sPeerTemp.indexOf("_DEV_") != 0) {
			sPeerTemp = ("_DEV_" + sPeer);
		}

		pgLibJNINode Node = m_Conf.GetNode();
		if (Node == null) {
			return;
		}

		Node.ObjectDelete(sPeerTemp);
	}


	public void Alert(String sTitle, String sMsg) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(sTitle);
		builder.setMessage(sMsg);
		builder.setPositiveButton("OK", null);
		builder.show();
	}

	public String getSDPath(){
		File sdDir = null;
		boolean sdCardExist = Environment.getExternalStorageState()
				.equals(android.os.Environment.MEDIA_MOUNTED);  //判断sd卡是否存在
		if  (sdCardExist)
		{
			sdDir = Environment.getExternalStorageDirectory();//获取跟目录
		}
		return sdDir.toString();

	}
	private int iflag=0;
	private android.view.View.OnClickListener m_OnClink = new android.view.View.OnClickListener() {
		// Control clicked
		public void onClick(View args0) {
			int k=0;
			switch (args0.getId()) {
				case R.id.btn_Start:
					if(bChair) {
						pgChairInit();
					}
					else {
						pgMembInit();
					}
					Log.d("OnClink", "init button");
					break;

				case R.id.btn_Clean:
					pgClean();
					Log.d("OnClink", "MemberAdd button");
					break;
				case R.id.btn_notifysend:
//					//Group_member869384011853858
//					m_Conf.MessageSend("hellllllo","Group_member869384011853858");
//					//pgNotifySend();
				{
//					String sPath = getSDPath() + "/Video.avi";
//					m_Conf.VideoRecord("_DEV_" + m_sChair, sPath);
//					Log.d("OnClink", "MemberAdd button");
//					break;
					m_Conf.CallSend("hello",m_sChair,"123");
					break;
				}
				case R.id.button:
//
					{
						//m_Conf.SvrRequest("(User){" + m_Conf.GetNode().omlEncode("_DEV_358180050453651_chairman")
						//		+ "}(Msg){" + m_Conf.GetNode().omlEncode("hello chairman") + "}");
						//m_Conf.AudioCtrlVolume(m_sChair,0,0);
						//m_Conf.Reset(m_sGroup,"Group_member869384011853858");

//						String sPath = getSDPath()+"/record.avi";
//						m_Conf.VideoRecord(m_sChair,sPath);
//						m_Conf.AudioRecord(m_sChair,sPath);
						for(int i =1 ;i<memberArray.size();i++) {
							m_Conf.AudioSpeech(memberArray.get(i).sPeer, true,false);
						}
						break;
//						if(iflag==0)
//						{
//							iflag++;
//							PeerAdd("123");
//						}
//						else
//						{
//							PeerDelete("123");
//							iflag=0;
//						}


//						if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
//							setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//						}
//						else if(getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
//						{
//							setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//						}
					}



//					SurfaceView view=m_Conf.VideoGetView(memberArray.get(1).sPeer);
//					if(view!=null)
//					{
//
//						Log.d("view test","view==null");
//					}
//					SurfaceView view1=m_Conf.VideoGetView(memberArray.get(2).sPeer);
//					if(view1!=null)
//					{
//
//						Log.d("view1 test","view==null");
//					}
//					SurfaceView view2=m_Conf.VideoGetView(memberArray.get(3).sPeer);
//					if(view2!=null)
//					{
//
//						Log.d("view2 test","view==null");
//					}

				default:

					break;
			}
		}
	};



//	public void CloseCap()
//	{
//		pgLibJNINode Node = m_Conf.GetNode();
//		if (Node != null) {
//			if (Node.ObjectAdd("_vTemp", "PG_CLASS_Video", "", 0)) {
//				int iErr=Node.ObjectRequest("_vTemp", 2, "(Item){9}(Value){" + 0+ "}", "");
//				Node.ObjectDelete("_vTemp");
//			}
//		}
//	}
//	public void OpenCap(){
//		pgLibJNINode Node = m_Conf.GetNode();
//		if (Node != null) {
//			if (Node.ObjectAdd("_vTemp", "PG_CLASS_Video", "", 0)) {
//				int iErr=Node.ObjectRequest("_vTemp", 2, "(Item){9}(Value){" + 1+ "}", "");
//				Node.ObjectDelete("_vTemp");
//			}
//		}
//	}
	//视频传输的状态
	private void EventVideoStatus(String sAct,String sData,String sPeer)
	{

	}
	//服务端下发的通知
	private void EventSvrNotify(String sAct,String sData,String sPeer)
	{

	}
	//发给服务端的消息的回执
	private void EventSvrReply(String sAct,String sData,String sPeer)
	{

	}
	//发给服务端的消息的回执
	private void EventSvrReplyError(String sAct,String sData,String sPeer)
	{

	}


	//登录的结果
	private void EventLogin(String sAct,String sData,String sPeer)
	{
		// Login reply
		// TODO: 2016/11/7 登录成功与否关系到后面的步骤能否执行 ，如果登录失败请再次初始化
		if(sData.equals("0"))
		{
			Log.d( "","已经登录");
		}
		else
		{
			Log.d( "","登录失败");
		}
	}

	//登出
	private void EventLogout(String sAct,String sData,String sPeer)
	{
		Log.d( "","已经注销"+sData);
	}
	//sPeer的离线消息
	private void EventPeerSync(String sAct,String sData,String sPeer)
	{
		// TODO: 2016/11/7 提醒应用程序可以和此节点相互发送消息了
		Log.d( "",sPeer+"节点建立连接");
	}
	//sPeer的离线消息
	private void EventPeerOffline(String sAct,String sData,String sPeer)
	{
		// TODO: 2016/11/7 提醒应用程序此节点离线了
		Log.d( "",sPeer+"节点离线");
	}

	//sPeer的离线消息
	private void EventChairmanSync(String sAct,String sData,String sPeer)
	{
		// TODO: 2016/11/7 提醒应用程序可以和主席发送消息了
		Log.d( "","主席节点建立连接");
		m_Conf.Join();
	}
	//sPeer的离线消息
	private void EventChairmanOffline(String sAct,String sData,String sPeer)
	{
		Log.d( "","主席节点离线");
	}
//-------------------------------------------------------------------------
	//sPeer的离线消息
	private void EventAskJoin(String sAct,String sData,String sPeer)
	{
		// TODO: 2016/11/7 sPeer请求加入会议  MemberAdd表示把他加入会议

		m_Conf.MemberAdd(sPeer);
	}
	//sPeer的离线消息
	private void EventJoin(String sAct,String sData,String sPeer)
	{
		// TODO: 2016/11/7 这里可以获取所有会议成员  可以尝试把sPeer加入会议成员表中

		if(sPeer.equals("_DEV_"+m_sUser))
		{
			//加入的是自己   启动视音频  ：
			m_Conf.VideoStart(pgVideoPutMode.Normal);
			m_Conf.AudioStart();
		}

		Log.d( "", sPeer+" 加入会议");
	}
	//sPeer的离线消息
	private void EventLeave(String sAct,String sData,String sPeer)
	{
		if(sPeer.equals("_DEV_"+m_sUser))
		{
			//离开的是自己   关闭视音频
			m_Conf.VideoStop();
			m_Conf.AudioStop();
			// 清理所有连接节点 数据
			int i=0;
			while (i<memberArray.size()){
				pgVideoRestore(memberArray.get(i).sPeer);
				i++;
			}
		}
		else {
			//清理这个节点播放视频相关的窗口 和绑定关系以及相关数据
			pgVideoRestore(sPeer);
		}
		Log.d( ""," 离开会议");
	}
//---------------------------------------------------------------------

	//sPeer的离线消息
	private void EventVideoSync(String sAct,String sData,String sPeer)
	{
        // TODO: 2016/11/7 提醒应用程序可以打开这个sPeer的视频了
		String sObjSelf="_DEV_"+m_sUser;
		if(sObjSelf.compareTo(sPeer)>0)
		{
			Log.d( ""," 发起视频请求");
			VideoOpen(sPeer);
		}
	}
	//sPeer的离线消息
	private void EventVideoSyncL(String sAct,String sData,String sPeer)
	{
		// TODO: 2016/11/7 提醒应用可以打开这个sPeer的第二种流
		Log.d( ""," 第二种视频同步");
	}
	private void EventVideoOpen(String sAct,String sData,String sPeer)
	{
		//收到视频请求
		Log.d( "",sPeer + " 请求视频");
		//// TODO: 2016/11/7 在这之后回复
		//调用
		VideoOpen(sPeer);
	}
	private void EventVideoLost(String sAct,String sData,String sPeer)
	{
		// TODO: 2016/11/8  对方视频已经丢失 挂断对方视频 并尝试重新打开
		Log.d("",sPeer + " 的视频已经丢失 尝试重新连接");
		//pgVideoClose(sPeer);

		//sleep(1000);

		//VideoOpen(sPeer);
	}
	private void EventVideoClose(String sAct,String sData,String sPeer)
	{
		// TODO: 2016/11/8  通知应用程序视频已经挂断
		Log.d("",sPeer + " 已经挂断视频");
		pgVideoRestore(sPeer);

	}
	private void EventVideoJoin(String sAct,String sData,String sPeer)
	{
		// TODO: 2016/11/8 请求端会收到请求打开视频的结果，打开视频成功除了显示和播放视频外，还有这个事件
		if (sData .equals("0") ) {
			Log.d("",sPeer + " 成功打开");
		} else {
			Log.d("", sPeer + " 打开失败");
			pgVideoRestore(sPeer);
		}
	}

	//-------------------------------------------------------------------
	//组消息
	private void EventNotify(String sAct,String sData,String sPeer)
	{

	}

	//sPeer的消息处理
	private void EventMessage(String sAct,String sData,String sPeer)
	{
		// TODO: 2016/11/7 处理sPeer发送过来的消息
		Log.d("",sPeer+":"+ sData);
	}


	private pgLibConference.OnEventListener m_OnEvent = new pgLibConference.OnEventListener() {

		@Override
		public void event(String sAct, String sData, final String sPeer) {
			// TODO Auto-generated method stub

			Log.d("pgLibConference", "OnEvent: Act=" + sAct + ", Data=" + sData + ", Peer=" + sPeer);

			if (sAct.equals("Login")) {
				EventLogin(sAct,sData,sPeer);
			}
			else if (sAct.equals("Logout")) {
				EventLogout(sAct,sData,sPeer);
			}
			else if (sAct .equals("PeerOffline")) {
				EventPeerOffline(sAct,sData,sPeer);
			}
			else if (sAct.equals("PeerSync")) {
				EventPeerSync(sAct,sData,sPeer);
			}
			else if (sAct .equals("ChairmanOffline")) {
				EventChairmanOffline(sAct,sData,sPeer);
			}
			else if (sAct.equals("ChairmanSync")) {
				EventChairmanSync(sAct,sData,sPeer);
			}
			else if(sAct.equals("AskJoin")) {
				EventAskJoin(sAct,sData,sPeer);
			}
			else if(sAct.equals("Join")){
				EventJoin(sAct,sData,sPeer);
			}
			else if(sAct.equals("Leave")){
                EventLeave(sAct,sData,sPeer);
			}
			else if(sAct.equals("VideoSync")) {
				EventVideoSync(sAct,sData,sPeer);
			}
			else if(sAct.equals("VideoSyncL")) {
				EventVideoSyncL(sAct,sData,sPeer);
			}
			else if (sAct.equals("VideoOpen")) {
				EventVideoOpen(sAct,sData,sPeer);
			}
			else if(sAct.equals("VideoLost")){
				EventVideoLost(sAct,sData,sPeer);
			}
			else if(sAct.equals("VideoClose")){
				EventVideoClose(sAct,sData,sPeer);
			}
			else if (sAct .equals( "VideoJoin")) {
				EventVideoJoin(sAct,sData,sPeer);
			}
			else if(sAct.equals("Message")) {
				EventMessage(sAct,sData,sPeer);
			}
			else if(sAct.equals("Notify")) {
				EventNotify(sAct,sData,sPeer);
			}
			else if(sAct.equals("SvrNotify")) {
				EventSvrNotify(sAct,sData,sPeer);
			}
			else if(sAct.equals("SvrReply")) {
				EventSvrReply(sAct,sData,sPeer);
			}
			else if(sAct.equals("SvrReplyError")) {
				EventSvrReplyError(sAct,sData,sPeer);
			}
		}
	};

	//选择成为主席端的初始化方式
	private void pgChairInit() {
		if(m_editText_name.getText().toString().equals(""))
		{
			Toast.makeText(this,"请在“主席ID”处输入一个测试ID",Toast.LENGTH_SHORT).show();
		}
		m_sGroup=m_editText_name.getText().toString().trim();
		m_editText_User.setText(m_sGroup);
		m_sChair=m_sGroup;
		m_sUser = m_sChair;
		//m_sUser = "";
		if ( m_sUser.equals("")||m_sChair.equals("")) {
			Log.e("Init", "Param Err");
			return;
		}

		if (!m_Conf.Initialize( m_sGroup, m_sChair, m_sUser, "",
				sSvr, "", m_sVideoParam, this)) {
			Log.d("pgConference", "Init failed");
			return;
		}
		PG_MEMB oMemb=memberArray.get(0);
		oMemb.sPeer=m_sUser;
		oMemb.pView = m_Conf.PreviewCreate(160, 120);
		oMemb.pLayout.addView(oMemb.pView);

	}


	//选择成为成员端的初始化方式
	private void pgMembInit() {
		if(m_editText_name.getText().toString().equals(""))
		{
			Toast.makeText(this,"请在“主席ID”处输入一个测试ID",Toast.LENGTH_SHORT).show();
			return;
		}
		if(m_editText_User.getText().toString().equals(""))
		{
			Toast.makeText(this,"请在“自己ID”处输入另一个测试ID",Toast.LENGTH_SHORT).show();
			return;
		}
		m_sGroup=m_editText_name.getText().toString().trim();
		m_sChair=m_sGroup;

		//为了方便演示以及 避免ID重复，做了随机数
//		Date d=new Date();
//		Random random = new Random(d.getTime());
		TelephonyManager tm = (TelephonyManager) this.getSystemService(TELEPHONY_SERVICE);

		@SuppressLint("HardwareIds") String IMEI =tm.getDeviceId();;
		m_sUser = m_editText_User.getText().toString().trim();
		if ( m_sUser.equals("")||m_sChair.equals("")) {
			Log.e("Init", "Param Err");
			return;
		}

		if (!m_Conf.Initialize( m_sGroup, m_sChair, m_sUser, "",
				sSvr, "", m_sVideoParam, this)) {
			Log.d("pgConference", "Init failed");
			return;
		}
		PG_MEMB oMemb=memberArray.get(0);
		oMemb.sPeer=m_sUser;
		oMemb.pView = m_Conf.PreviewCreate(160, 120);
		oMemb.pLayout.addView(oMemb.pView);
	}
	//结束会议模块
	private void pgClean()
	{
		for(int i=0;i<memberArray.size();i++)
		{
			PG_MEMB oMemb=memberArray.get(i);
			if(!oMemb.sPeer.equals(""))
				m_Conf.VideoClose(oMemb.sPeer);
			oMemb.pLayout.removeAllViews();
			oMemb.pView=null;
			oMemb.sPeer="";

		}
		m_Conf.Clean();

	}
	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		if(isChecked)
		{
			bChair=true;
			m_editText_User.setEnabled(false);
		}
		else
		{
			bChair=false;
			m_editText_User.setEnabled(true);
		}
	}

	//搜索成员列表
	PG_MEMB MembSearch(String sPeer) {

		try
		{
			if (sPeer .equals("")) {
				Log.d("","Search can't Search Start");
				return null;
			}

			for (int i = 0; i < memberArray.size(); i++) {

				if (memberArray.get(i).sPeer .equals(sPeer)||memberArray.get(i).sPeer.equals("")||memberArray.get(i).sPeer==null) {
					return memberArray.get(i);
				}
			}

		}
		catch (Exception ex) {
			Log.d("","VideoOption. ex=" + ex.toString());
		}
		return null;
	}

	//打开视频时完成窗口和相关数据的改变
	private boolean VideoOpen(String sPeer) {

		PG_MEMB MembTmp = MembSearch(sPeer);
		if(MembTmp==null)
		{
			m_Conf.VideoOpen(sPeer,0,0);
			return false;
		}
		if(MembTmp.sPeer.equals(""))
		{
			MembTmp.sPeer=sPeer;
		}

		MembTmp.pView=m_Conf.VideoOpen(sPeer,160,120);
		if(MembTmp.pView!=null)
		{
			MembTmp.pLayout.addView(MembTmp.pView);
		}

		return true;

	}

	/*
	* 重置节点的显示窗口
	* */
	private void pgVideoRestore(String sPeer)
	{
		PG_MEMB MembTmp = MembSearch(sPeer);
		if(MembTmp!=null)
		{
			if(MembTmp.sPeer.equals(sPeer))
			{
				MembTmp.pLayout.removeView(MembTmp.pView);
				MembTmp.pView=null;
				MembTmp.sPeer="";
			}
		}
	}

	//清理窗口数据和关闭视频
	private void pgVideoClose(String sPeer)
	{
		pgVideoRestore(sPeer);
		m_Conf.VideoClose(sPeer);
	}
	//给所有加入会议的成员发送消息
	private boolean pgNotifySend()
	{
		String sData=m_editText_Notify.getText().toString();
		if(sData.equals(""))
		{
			return false;
		}
		m_Conf.NotifySend(sData);

		return true;
	}

	private boolean m_bSpeechEnable = true;

	//选择自己的声音是否在对端播放
	private void Speech() {
		if (!m_Conf.AudioSpeech(m_sMemb,m_bSpeechEnable)) {
			Log.d("pgRobotClient", "Enable speech failed");
		} else {
			if (m_bSpeechEnable) {
				m_bSpeechEnable = false;
			} else {
				m_bSpeechEnable = true;
			}
		}
	}

}


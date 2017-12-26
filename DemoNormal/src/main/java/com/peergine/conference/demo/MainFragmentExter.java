//package com.peergine.conference.demo;
//
//import android.app.Activity;
//import android.app.AlertDialog;
//import android.content.DialogInterface;
//import android.content.Intent;
//import android.hardware.Camera;
//import android.os.Bundle;
//import android.os.Environment;
//import android.util.Log;
//import android.view.KeyEvent;
//import android.view.Menu;
//import android.view.MenuItem;
//import android.view.SurfaceView;
//import android.view.View;
//import android.view.WindowManager;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.LinearLayout;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import com.peergine.android.conference.pgLibConference;
//import com.peergine.android.conference.pgLibTimer;
//import com.peergine.plugin.exter.VideoAudioInputExternal;
//import com.peergine.plugin.lib.pgLibJNINode;
//
//import java.io.File;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Date;
//
//import me.yokeyword.fragmentation.SupportFragment;
//
//import static com.peergine.android.conference.pgLibConference.*;
//import static com.peergine.android.conference.pgLibConferenceEvent.*;
//
///*
//*
//* TODO: + 说明：
//* 如果代码中有该标识，说明在标识处有功能代码待编写，待实现的功能在说明中会简略说明。
//* FIXME: + 说明：
//* 如果代码中有该标识，说明标识处代码需要修正，甚至代码是错误的，不能工作，需要修复，如何修正会在说明中简略说明。
//*
//* XXX : + 说明：
//* 如果代码中有该标识，说明标识处代码虽然实现了功能，但是实现的方法有待商榷，希望将来能改进，要改进的地方会在说明中简略说明。
//*
//*
//*
//* */
//
//
//
///*
// * Updata 2017 02 15 V13
// * 添加定时器的使用示范
// * 修改VideoStart  AudioStart  VideoOpen 的使用时机示范。
// *
// *
// */
//
//public class MainFragmentExter extends SupportFragment {
//
//	private String m_sGroup = "";
//	private String m_sChair = "";
//
//	private String m_sUser = "";
//	private String m_sPass = "";
//
//	private String m_sSvrAddr ="connect.peergine.com:7781";
//	private String m_sRelayAddr = "";
//	private String m_sVideoParam=
//			"(Code){3}(Mode){2}(FrmRate){40}" +
//					"(LCode){3}(LMode){3}(LFrmRate){30}" +
//					"(Portrait){0}(Rotate){0}(BitRate){300}(CameraNo){"+ Camera.CameraInfo.CAMERA_FACING_FRONT+"}"+
//					"(AudioSpeechDisable){0}";
//	private String m_sMemb = "";
//
//	private pgLibConference m_Conf = null;
//	private pgLibJNINode m_Node = null;
//
//	private int RIDLaout[]={R.id.layoutVideoS1, R.id.layoutVideoS2,R.id.layoutVideoS3};
//
//	private EditText m_editchair =null;
//	private Button m_btnStart = null;
//	private Button m_btnStop = null;
//	private Button m_btnClean = null;
//	private Button m_btnLanScan = null;
//
//	private EditText m_editText_Notify =null;
//	private Button m_btnNotifySend= null;
//	private TextView text_info = null;
//	private Button m_btntest=null;
//    private Button m_btn_recordstart=null;
//    private Button m_btn_recordstop=null;
//
//	private LinearLayout PreviewLayout=null;
//	private SurfaceView m_Preview=null;
//	private Button m_BtnClearlog = null;
//
//	//R.id.layoutVideoS0,
//
//	class PG_MEMB{
//		String sPeer="";
//		Boolean bVideoSync=false;
//		Boolean bJoin=false;
//		SurfaceView pView=null;
//		LinearLayout pLayout=null;
//	}
//
//	private static ArrayList<PG_MEMB> memberArray = new ArrayList<>();
//
//
//	//定时器例子 超时处理实现
//	final pgLibTimer timer = new pgLibTimer();
//
//	final pgLibTimer.OnTimeOut timerOut = new pgLibTimer.OnTimeOut() {
//		@Override
//		public void onTimeOut(String sParam) {
//			if (m_Node == null) {
//				return;
//			}
//			//中间件oml 格式数据解析示例
//			String sAct = m_Node.omlGetContent(sParam, "Act");
//			String sPeer = m_Node.omlGetContent(sParam, "Peer");
//
//			//执行打开视频的动作
//			if ("VIDEO_OPEN".equals(sAct)) {
//
//				//Demo 是为了演示方便 在这里实现自动打开视频的功能
//				//所以才做了这个ID大的主动打开视频
//				//实际情况中建议从Join出得到设备列表，或者本地保存列表，用ListView显示，点击某个ID然后开始打开视频
//
//				if (m_sUser.compareTo(sPeer) > 0) {
//					showInfo(" 发起视频请求");
//					pgVideoOpen(sPeer);
//				}
//			}
//		}
//	};
//
//	public static MainFragmentCalling newInstance(String sUser,String sPass,String sSvrAddr,String sRelayAddr,
//												  String sInitParam,String sVideoParam,String sExpire){
//		MainFragmentCalling fragment = new MainFragmentCalling();
//		Bundle args = new Bundle();
//		args.putString("User", sUser);
//		args.putString("Pass", sPass);
//		args.putString("SvrAddr", sSvrAddr);
//		args.putString("RelayAddr", sRelayAddr);
//		args.putString("InitParam", sInitParam);
//		args.putString("VideoParam", sVideoParam);
//		args.putString("Expire", sExpire);
//		fragment.setArguments(args);
//		return fragment;
//	}
//
//	@Override
//	public void onCreate(Bundle savedInstanceState) {
//		super.onCreate(savedInstanceState);
//
////		Camera camera=null;
////		setCameraDisplayOrientation(this,Camera.CameraInfo.CAMERA_FACING_FRONT,camera);
//		memberArray.clear();
//		//todo 4个窗口初始化
//		PreviewLayout = (LinearLayout) findViewById(R.id.layoutVideoS0);
//		for (int aRIDLaout : RIDLaout) {
//			PG_MEMB oMemb = new PG_MEMB();
//			oMemb.pLayout = (LinearLayout) findViewById(aRIDLaout);
//			memberArray.add(oMemb);
//		}
////		TextView textView = (TextView)findViewById(R.id.text_notuse);
////		textView.requestFocus();
//
//		//初始化控件
//
//		m_editchair = (EditText) findViewById(R.id.editText_chair);
//
//		m_btnStart = (Button) findViewById(R.id.btn_Start);
//		m_btnStart.setOnClickListener(m_OnClink);
//
//		m_btnStop = (Button) findViewById(R.id.btn_stop);
//		m_btnStop.setOnClickListener(m_OnClink);
//
//		m_btnClean =(Button) findViewById(R.id.btn_Clean);
//		m_btnClean.setOnClickListener(m_OnClink);
//
//		m_btnLanScan = (Button)findViewById(R.id.btn_LanScan);
//		m_btnLanScan.setOnClickListener(m_OnClink);
//
//		m_editText_Notify =(EditText)findViewById(R.id.editText_notify);
//
//		m_btnNotifySend=(Button) findViewById(R.id.btn_notifysend);
//		m_btnNotifySend.setOnClickListener(m_OnClink);
//
//		m_btntest=(Button) findViewById(R.id.button);
//		m_btntest.setOnClickListener(m_OnClink);
//
//        m_btn_recordstart=(Button) findViewById(R.id.btn_recordstart);
//        m_btn_recordstart.setOnClickListener(m_OnClink);
//
//        m_btn_recordstop=(Button) findViewById(R.id.btn_recordstop);
//        m_btn_recordstop.setOnClickListener(m_OnClink);
//
//		m_BtnClearlog = (Button) findViewById(R.id.btn_clearlog);
//		m_BtnClearlog.setOnClickListener(m_OnClink);
//		//显示一些信息
//		text_info= (TextView) findViewById(R.id.text_info);
//
//
////		String sConfig_Node = "Type=0;Option=1;MaxPeer=256;MaxGroup=32;MaxObject=512;MaxMCast=512;MaxHandle=256;SKTBufSize0=128;SKTBufSize1=64;SKTBufSize2=256;SKTBufSize3=64";
//		Intent intent = getIntent();
//		m_sUser = 			intent.getStringExtra("User");
//		m_sPass =			intent.getStringExtra("User");
//		m_sSvrAddr = 		intent.getStringExtra("SvrAddr");
//		m_sRelayAddr = 	intent.getStringExtra("RelayAddr");
//		m_sVideoParam = 	intent.getStringExtra("VideoParam");
//
//		int iExpire = 		ParseInt(intent.getStringExtra("Expire"),10);
//		int iMaxPeer = 		ParseInt(intent.getStringExtra("MaxPeer"),256);
//		int iMaxObject = 	ParseInt(intent.getStringExtra("MaxObject"),512);
//		int iMaxMCast = 	ParseInt(intent.getStringExtra("MaxMCast"),512);
//		int iMaxHandle = 	ParseInt(intent.getStringExtra("MaxHandle"),256);
//
//		if(m_Conf==null){
//			m_Conf= new pgLibConference();
//			m_Conf.SetEventListener(m_OnEvent);
//
//			m_Conf.SetExpire(iExpire);
//			PG_NODE_CFG mNodeCfg = new PG_NODE_CFG();
//
//				mNodeCfg.MaxPeer = iMaxPeer;
//				mNodeCfg.MaxObject = iMaxObject;
//				mNodeCfg.MaxMCast = iMaxMCast;
//				mNodeCfg.MaxHandle= iMaxHandle;
//			m_Conf.ConfigNode(mNodeCfg);
//
//		}
//		m_sVideoParam += "(VideoInExternal){1}";
//
//		if(!m_Conf.Initialize(m_sUser,m_sPass,m_sSvrAddr,m_sRelayAddr,m_sVideoParam,this)) {
//			Log.d("pgConference", "Init failed");
//			AlertDialog.Builder builder = new AlertDialog.Builder(this);
//			builder.setTitle("Error");
//			builder.setMessage("请安装pgPlugin xx.APK 或者检查网络状况!");
//			builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//				@Override
//				public void onClick(DialogInterface dialog, int which) {
//					finish();
//				}
//			});
//			return;
//		}
//		m_Preview= m_Conf.PreviewCreate(160, 120);
//		VideoAudioInputExternal external = new VideoAudioInputExternal(m_Conf.GetNode(),PreviewLayout,this);
//		external.VideoInputExternalEnable();
//
////		PreviewLayout.removeAllViews();
////		PreviewLayout.addView(m_Preview);
//
//
////		SetVolumeGate(1);
////		SetAudioSuppress(0,0,0);
//		//初始化定时器
//		m_Node= m_Conf.GetNode();
//		m_Conf.TimerOutAdd(timerOut);
//	}
//
//	@Override
//	public boolean onKeyUp(int keyCode, KeyEvent event) {
//		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
//			Log.d("pgLiveCapture", "onKeyDown, KEYCODE_BACK");
//			ExitDialog();
//			return true;
//		}
//		return super.onKeyUp(keyCode, event);
//	}
//
//	private DialogInterface.OnClickListener m_DlgClick = new DialogInterface.OnClickListener() {
//		public void onClick(DialogInterface dialog, int which) {
//			if (which == AlertDialog.BUTTON_POSITIVE) {
//				pgStop();
//				m_Conf.Clean();
//				android.os.Process.killProcess(android.os.Process.myPid());
//			}
//		}
//	};
//
//	public void ExitDialog() {
//		AlertDialog.Builder builder = new AlertDialog.Builder(this);
//		builder.setTitle("Confirm");
//		builder.setMessage("Are you sure to exit?");
//		builder.setPositiveButton("YES", m_DlgClick);
//		builder.setNegativeButton("NO", m_DlgClick);
//		builder.show();
//	}
//	private int m_iAngle=0;
//	public void SetRotate(int iAngle) {
//		pgLibJNINode Node = m_Conf.GetNode();
//		if (Node != null) {
//			if (Node.ObjectAdd("_vTemp", "PG_CLASS_Video", "", 0)) {
//				Node.ObjectRequest("_vTemp", 2, "(Item){2}(Value){" + iAngle + "}", "");
//				Node.ObjectDelete("_vTemp");
//			}
//		}
//	}
//
//
//	//屏幕旋转 设置摄像头旋转角度
////	@Override
////	public void onConfigurationChanged(Configuration newConfig) {
////			 super.onConfigurationChanged(newConfig);
////			// 检测屏幕的方向：纵向或横向
////
////			if (this.getResources().getConfiguration().orientation
////				== Configuration.ORIENTATION_LANDSCAPE) {
////				//当前为横屏， 在此处添加额外的处理代码
////				SetRotate(0);
////				// todo 或者添加 为180度
////			}
////			else if (this.getResources().getConfiguration().orientation
////				== Configuration.ORIENTATION_PORTRAIT) {
////				//当前为竖屏， 在此处添加额外的处理代码
////				SetRotate(90);
////			}
////		}
//
//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		// Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater().inflate(R.menu.main, menu);
//		return true;
//	}
//
//	@Override
//	public boolean onOptionsItemSelected(MenuItem item) {
//		// Handle action bar item clicks here. The action bar will
//		// automatically handle clicks on the Home/Up button, so long
//		// as you specify a parent activity in AndroidManifest.xml.
//		int id = item.getItemId();
//		return id == R.id.action_settings || super.onOptionsItemSelected(item);
//	}
//
//	@Override
//	public void onDestroy() {
//		super.onDestroy();
//	}
//
//	// 设置设备的麦克风的采样率。
//	// 在初始化成功之后，打开音频通话之前调用。
//	// iRate: 采样率，单位HZ/秒。有效值：8000, 1600, 32000
//	// 示范利用中间件编写扩展
//	public void SetAudioSampleRate(int iRate) {
//		pgLibJNINode Node = m_Conf.GetNode();
//		if (Node != null) {
//			// Set microphone sample rate
//			if (Node.ObjectAdd("_AudioTemp", "PG_CLASS_Audio", "", 0)) {
//				Node.ObjectRequest("_AudioTemp", 2, "(Item){2}(Value){" + iRate + "}", "");
//				Node.ObjectDelete("_AudioTemp");
//			}
//		}
//	}
//
//	public void Alert(String sTitle, String sMsg) {
//		AlertDialog.Builder builder = new AlertDialog.Builder(this);
//		builder.setTitle(sTitle);
//		builder.setMessage(sMsg);
//		builder.setPositiveButton("OK", null);
//		builder.show();
//	}
//
//	public String getSDPath(){
//		File sdDir = null;
//		boolean sdCardExist = Environment.getExternalStorageState()
//				.equals(Environment.MEDIA_MOUNTED);  //判断sd卡是否存在
//		if  (sdCardExist)
//		{
//			sdDir = Environment.getExternalStorageDirectory();//获取跟目录
//		}
//
//		return (sdDir==null)?"":sdDir.toString();
//
//	}
//	private void Show(String s){
//		String sInfo = text_info.getText()+"\n"+s;
//		text_info.setText(sInfo);
//	}
//
//	private int iflag=0;
//
//	public boolean SetVolumeGate(int iVolumeGate)
//	{
//		pgLibJNINode Node = m_Conf.GetNode();
//		if (Node != null) {
//			if (Node.ObjectAdd("_aTemp", "PG_CLASS_Audio", "", 0)) {
//				String sValue = Node.omlEncode("(TailLen){0}(VolGate){" + iVolumeGate + "}");
//				Node.ObjectRequest("_aTemp", 2, "(Item){3}(Value){" + sValue + "}", "");
//				Node.ObjectDelete("_aTemp");
//				return true;
//			}
//		}
//		return false;
//	}
//
//	// 设置杂音抑制参数。
//	// 在初始化成功之后，打开音频通话之前调用。
//	// iDebug: 是否打开调试信息：1:打开调试，0:关闭调试
//	// iDelay: 延时的音频帧数
//	// iKeep: 抑制的音频帧数
//	//
//	public void SetAudioSuppress(int iDebug, int iDelay, int iKeep) {
//		pgLibJNINode Node = m_Conf.GetNode();
//		if (Node != null) {
//			// Set microphone sample rate
//			if (Node.ObjectAdd("_AudioTemp", "PG_CLASS_Audio", "", 0)) {
//				String sValue = "(Debug){" + iDebug + "}(Delay){" + iDelay + "}(Keep){" + iKeep + "}";
//				Node.ObjectRequest("_AudioTemp", 2, "(Item){0}(Value){" + Node.omlEncode(sValue) + "}", "");
//				Node.ObjectDelete("_AudioTemp");
//			}
//		}
//	}
//
//
//	private View.OnClickListener m_OnClink = new View.OnClickListener() {
//		// Control clicked
//		public void onClick(View args0) {
//			int k=0;
//			switch (args0.getId()) {
//				case R.id.btn_Start:
//					m_bVideoStart = false;
//					pgStart();
//					m_btnStart.setEnabled(false);
//					Log.d("OnClink", "init button");
//					break;
//				case R.id.btn_stop:
//					pgStop();
//					m_btnStart.setEnabled(true);
//					Log.d("OnClink", "MemberAdd button");
//					break;
//				case R.id.btn_Clean:
//					pgClean();
//					m_btnStart.setEnabled(true);
//					Log.d("OnClink", "MemberAdd button");
//					break;
//				case R.id.btn_LanScan:
//					m_Conf.LanScanStart();
//					break;
//
//				case R.id.btn_notifysend:
////					//Group_member869384011853858
////					m_Conf.MessageSend("hellllllo","Group_member869384011853858");
////					//pgNotifySend();
//				{
//
////					String sPath = getSDPath() + "/Video.avi";
////					m_Conf.VideoRecord("_DEV_" + m_sChair, sPath);
////					Log.d("OnClink", "MemberAdd button");
////					break;
//					m_Conf.CallSend("hello",m_sChair,"123");
//					break;
//				}
//				case R.id.button:
////              test Api
//					{
//						//m_Conf.SvrRequest("(User){" + m_Conf.GetNode().omlEncode("_DEV_358180050453651_chairman")
//						//		+ "}(Msg){" + m_Conf.GetNode().omlEncode("hello chairman") + "}");
//						//m_Conf.AudioCtrlVolume(m_sChair,0,0);
//						//m_Conf.Reset(m_sGroup,"Group_member869384011853858");
////                        Date currentTime = new Date();
////                        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
////                        String sDate = formatter.format(currentTime);
////						String sPath = getSDPath()+"/record"+sDate+".avi";
////						m_Conf.RecordStart(m_sChair,sPath);
////						m_Conf.AudioRecordStart(m_sChair,sPath);
//
//						break;
//
//					}
//                case R.id.btn_recordstart:{
//                    Date currentTime = new Date();
//                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
//                    String sDate = formatter.format(currentTime);
//                    String sPath = getSDPath()+"/record"+sDate+".avi";
//                    if((!m_Conf.RecordStart(m_sChair,sPath))){
//						Toast.makeText(getApplication(),"录像失败。 已经关闭",Toast.LENGTH_SHORT).show();
//						m_Conf.RecordStop(m_sChair);
////						m_Conf.AudioRecordStop(m_sChair);
//                    }
//					break;
//                }
//                case R.id.btn_recordstop:{
//                    m_Conf.RecordStop(m_sChair);
//                    //m_Conf.AudioRecordStop(m_sChair);
//                    break;
//                }
//				case R.id.btn_clearlog:{
//					text_info.setText("");
//				}
//				default:
//
//					break;
//			}
//		}
//	};
//
//
//
//	//视频传输的状态
//	private void EventVideoFrameStat(String sAct,String sData,String sPeer)
//	{
//		//Show()
//
//	}
//	//服务端下发的通知
//	private void EventSvrNotify(String sAct,String sData,String sPeer)
//	{
//		Show("SvrNotify :"+sData+" : "+sPeer);
//	}
//	//发给服务端的消息的回执
//	private void EventSvrReply(String sAct,String sData,String sPeer)
//	{
//
//	}
//	//发给服务端的消息的回执
//	private void EventSvrReplyError(String sAct,String sData,String sPeer)
//	{
//
//	}
//
//
//	//登录的结果
//	private void EventLogin(String sAct,String sData,String sPeer)
//	{
//		// Login reply
//		// TODO: 2016/11/7 登录成功与否关系到后面的步骤能否执行 ，如果登录失败请再次初始化
//		if(sData.equals("0"))
//		{
//			Show("已经登录");
//			Log.d( "","已经登录");
//		}
//		else
//		{
//			Show("登录失败 err = "+sData);
//			Log.d( "","登录失败");
//		}
//	}
//
//	//登出
//	private void EventLogout(String sAct,String sData,String sPeer)
//	{
//		Show( "已经注销"+sData);
//	}
//
//	//保存初次连接 ,如果程序崩溃，
//
//	//sPeer的离线消息
//	private void EventPeerSync(String sAct,String sData,String sPeer)
//	{
//		// TODO: 2016/11/7 提醒应用程序可以和此节点相互发送消息了
//		Show(sPeer+"节点建立连接");
//
//		m_Conf.MessageSend("MessageSend test",sPeer);
//		m_Conf.CallSend("CallSend test",sPeer,"123");
//
//	}
//	//sPeer的离线消息
//	private void EventPeerOffline(String sAct,String sData,String sPeer)
//	{
//		// TODO: 2016/11/7 提醒应用程序此节点离线了
//		Show( sPeer+"节点离线 sData = "+sData);
//	}
//
//	//sPeer的离线消息
//	private void EventChairmanSync(String sAct,String sData,String sPeer)
//	{
//		// TODO: 2016/11/7 提醒应用程序可以和主席发送消息了
//		Show( "主席节点建立连接 Act = "+sAct+" : "+sData + " : "+sPeer);
//		m_Conf.Join();
//		m_Conf.MessageSend("MessageSend test",sPeer);
//		m_Conf.CallSend("CallSend test",sPeer,"123");
//	}
//	//sPeer的离线消息
//	private void EventChairmanOffline(String sAct,String sData,String sPeer)
//	{
//		Show( "主席节点离线 sData = "+sPeer);
//	}
////-------------------------------------------------------------------------
//	//sPeer的离线消息
//
//	private void EventAskJoin(String sAct,String sData,String sPeer)
//	{
//		// TODO: 2016/11/7 sPeer请求加入会议  MemberAdd表示把他加入会议
//		Show( sPeer+"请求加入会议->同意");
//		m_Conf.MemberAdd(sPeer);
//	}
//	//sPeer的离线消息
//	private boolean m_bVideoStart =false;
//	private void EventJoin(String sAct,String sData,String sPeer)
//	{
//		// TODO: 2016/11/7 这里可以获取所有会议成员  可以尝试把sPeer加入会议成员表中
//		Show( sPeer+"加入会议");
//		String sParam  = "(Act){DoVideoOpen}(Peer){"+sPeer+"}";
//		m_Conf.TimerStart(sParam,1,false);
//
//		m_Conf.NotifySend(sPeer + " : join ");
//		Log.d( "", sPeer+" 加入会议");
//	}
//	//sPeer的离线消息
//	private void EventLeave(String sAct,String sData,String sPeer)
//	{
//		Show( sPeer+"离开会议");
//
//		Log.d( ""," 离开会议");
//	}
////---------------------------------------------------------------------
//
//	//sPeer的离线消息
//	private void EventVideoSync(String sAct,String sData,String sPeer)
//	{
//        // TODO: 2016/11/7 提醒应用程序可以打开这个sPeer的视频了
//		Show("视频同步");
//	}
//	//sPeer的离线消息
//	private void EventVideoSyncL(String sAct,String sData,String sPeer)
//	{
//		// TODO: 2016/11/7 提醒应用可以打开这个sPeer的第二种流
//		Log.d( ""," 第二种视频同步");
//		Show("第二种视频同步");
//	}
//	private void EventVideoOpen(String sAct,String sData,String sPeer)
//	{
//		//收到视频请求
//		Show( sPeer + " 请求视频连线->同意");
//		//// TODO: 2016/11/7 在这之后回复
//		//调用
//		pgVideoOpen(sPeer);
//	}
//
//	private void EventVideoLost(String sAct, String sData, final String sPeer)
//	{
//		// TODO: 2016/11/8  对方视频已经丢失 挂断对方视频 并尝试重新打开
//		Show(sPeer + " 的视频已经丢失 可以尝试重新连接");
//	}
//
//
//	private void EventVideoClose(String sAct,String sData,String sPeer)
//	{
//		// TODO: 2016/11/8  通知应用程序视频已经挂断
//		Show(sPeer + " 已经挂断视频");
//		pgVideoRestore(sPeer);
//
//	}
//	private void EventVideoJoin(String sAct,String sData,String sPeer)
//	{
//		// TODO: 2016/11/8 请求端会收到请求打开视频的结果，打开视频成功除了显示和播放视频外，还有这个事件
//		if (sData .equals("0") ) {
//			Show(sPeer+":"+ "视频成功打开");
//			Log.d("",sPeer + " 成功打开");
//		} else {
//			Show(sPeer+":"+ "视频成功失败");
//			Log.d("", sPeer + " 打开失败");
//			pgVideoRestore(sPeer);
//		}
//	}
//
//	//-------------------------------------------------------------------
//	//组消息
//	private void EventNotify(String sAct,String sData,String sPeer)
//	{
//		Show(sPeer + ": sData = "+sData);
//	}
//
//	//sPeer的消息处理
//	private void EventMessage(String sAct,String sData,String sPeer)
//	{
//		// TODO: 2016/11/7 处理sPeer发送过来的消息
//		Show(sPeer+":"+ sData);
//		Log.d("",sPeer+":"+ sData);
//	}
//
//
//	private OnEventListener m_OnEvent = new OnEventListener() {
//
//		@Override
//		public void event(String sAct, String sData, final String sPeer) {
//			// TODO Auto-generated method stub
//
//			Log.d("ConferenceDemo", "OnEvent: Act=" + sAct + ", Data=" + sData + ", Peer=" + sPeer);
//			if (sAct.equals(EVENT_VIDEO_FRAME_STAT)) {
//				EventVideoFrameStat(sAct,sData,sPeer);
//			}
//			else if (sAct.equals(EVENT_LOGIN)) {
//				EventLogin(sAct,sData,sPeer);
//			}
//			else if (sAct.equals(EVENT_LOGOUT)) {
//				EventLogout(sAct,sData,sPeer);
//			}
//			else if (sAct .equals(EVENT_PEER_OFFLINE)) {
//				// PeerAdd 后消息发送如果对端离线会产生这个消息
//				EventPeerOffline(sAct,sData,sPeer);
//			}
//			else if (sAct.equals(EVENT_PEER_SYNC)) {
//				//PeerAdd 后如果成功会在两端产生这个同步消息
//				EventPeerSync(sAct,sData,sPeer);
//			}
//			else if (sAct .equals(EVENT_CHAIRMAN_OFFLINE)) {
//				EventChairmanOffline(sAct,sData,sPeer);
//			}
//			else if (sAct.equals(EVENT_CHAIRMAN_SYNC)) {
//				EventChairmanSync(sAct,sData,sPeer);
//			}
//			else if(sAct.equals(EVENT_ASK_JOIN)) {
//				EventAskJoin(sAct,sData,sPeer);
//			}
//			else if(sAct.equals(EVENT_JOIN)){
//				EventJoin(sAct,sData,sPeer);
//			}
//			else if(sAct.equals(EVENT_LEAVE)){
//				EventLeave(sAct,sData,sPeer);
//			}
//			else if(sAct.equals(EVENT_VIDEO_SYNC)) {
//				EventVideoSync(sAct,sData,sPeer);
//			}
//			else if(sAct.equals(EVENT_VIDEO_SYNC_1)) {
//				EventVideoSyncL(sAct,sData,sPeer);
//			}
//			else if (sAct.equals(EVENT_VIDEO_OPEN)) {
//				EventVideoOpen(sAct,sData,sPeer);
//			}
//			else if(sAct.equals(EVENT_VIDEO_LOST)){
//				EventVideoLost(sAct,sData,sPeer);
//			}
//			else if(sAct.equals(EVENT_VIDEO_CLOSE)){
//				EventVideoClose(sAct,sData,sPeer);
//			}
//			else if (sAct .equals(EVENT_VIDEO_JOIN)) {
//				EventVideoJoin(sAct,sData,sPeer);
//			}
//			else if (sAct .equals(EVENT_VIDEO_CAMERA)) {
//				EventVideoCamera(sAct,sData,sPeer);
//			}
//			else if (sAct .equals( EVENT_VIDEO_RECORD)) {
//				EventVideoRecord(sAct,sData,sPeer);
//			}
//			else if(sAct.equals(EVENT_MESSAGE)) {
//				// 收到sPeer 调用MessageSend 后产生的消息事件
//				EventMessage(sAct,sData,sPeer);
//			}
//			else if(sAct.equals(EVENT_CALLSEND_RESULT)) {
//				EventCallSend(sAct,sData,sPeer);
//			}
//			else if(sAct.equals(EVENT_NOTIFY)) {
//				EventNotify(sAct,sData,sPeer);
//			}
//			else if(sAct.equals(EVENT_SVR_NOTIFY)) {
//				// 收到服务器下推的消息
//				EventSvrNotify(sAct,sData,sPeer);
//			}
//			else if(sAct.equals(EVENT_SVR_RELAY)) {
//				// 给服务器发送消息后产生的回执
//				EventSvrReply(sAct,sData,sPeer);
//			}
//			else if(sAct.equals(EVENT_SVR_REPLYR_ERROR)) {
//				// 给服务器发送消息后产生的错误
//				EventSvrReplyError(sAct,sData,sPeer);
//			}
//			else if(sAct.equals(EVENT_LAN_SCAN_RESULT)){
//				EventLanScanResult(sAct,sData,sPeer);
//			}
//		}
//	};
//
//	private void EventCallSend(String sAct, String sData, String sPeer) {
//		// CallSend （具有回执的信息） 最终结果
//		Show("CallSend 回执 sData = "+sData);
//	}
//
//	private void EventVideoRecord(String sAct, String sData, String sPeer) {
//		// VideoRecord 视频录制的结果
//	}
//
//	private void EventVideoCamera(String sAct, String sData, String sPeer) {
//		// VideoCamera 视频拍照的结果
//	}
//	private void EventLanScanResult(String sAct,String sData,String sPeer){
//		Show("Act : LanScanResult  -- sData: "+sData + "  sPeer  : "+sPeer);
//	}
//
//	private static int ParseInt(String sInt, int iDef) {
//		try {
//			if (sInt.equals("")) {
//				return 0;
//			}
//			return Integer.parseInt(sInt);
//		} catch (Exception ex) {
//			return iDef;
//		}
//	}
//
//	private void pgStart(){
//		m_sChair = m_editchair.getText().toString().trim();
//		if(m_sChair.equals("")){
//			Alert("错误","主席端ID不能为空。");
//		}
//		String sName = m_sChair;
//		m_Conf.Start(sName,m_sChair);
//		m_Conf.VideoStart(VIDEO_NORMAL);
//		m_Conf.AudioStart();
//	}
//
//	private void pgStop(){
//
//		for(int i=0;i<memberArray.size();i++)
//		{
//			PG_MEMB oMemb=memberArray.get(i);
//			if(!oMemb.sPeer.equals("")) {
//				pgVideoClose(oMemb.sPeer);
//			}
//		}
//		m_Conf.AudioStop();
//		m_Conf.VideoStop();
//		m_Conf.Stop();
//
//	}
//
//
//	//结束会议模块
//	private void pgClean()
//	{
//		pgStop();
//		m_Conf.TimerOutDel(timerOut);
//		m_Node=null;
//
//		//m_Conf.PreviewDestroy();
//		m_Conf.Clean();
//		finish();
////		m_PeerLink.clear();
//	}
//
//	//搜索成员列表
//	PG_MEMB MembSearch(String sPeer) {
//
//		try
//		{
//			if (sPeer .equals("")) {
//				Log.d("","Search can't Search Start");
//				return null;
//			}
//			for (int i = 0; i < memberArray.size(); i++) {
//
//				if (memberArray.get(i).sPeer .equals(sPeer)||memberArray.get(i).sPeer .equals("")) {
//					return memberArray.get(i);
//				}
//			}
//
//		}
//		catch (Exception ex) {
//			Log.d("","VideoOption. ex=" + ex.toString());
//		}
//		return null;
//	}
//
//	//打开视频时完成窗口和相关数据的改变
//	private boolean pgVideoOpen(String sPeer) {
//
//		PG_MEMB MembTmp = MembSearch(sPeer);
//		//没有窗口了
//		if(MembTmp==null)
//		{
//			m_Conf.VideoReject(sPeer);
//			return false;
//		}
//		if(MembTmp.sPeer.equals(""))
//		{
//			MembTmp.sPeer=sPeer;
//		}
//
//		MembTmp.pView=m_Conf.VideoOpen(sPeer,160,120);
//		if(MembTmp.pView!=null)
//		{
//			MembTmp.pLayout.removeAllViews();
//			MembTmp.pLayout.addView(MembTmp.pView);
//		}
//
//		return true;
//
//	}
//
//	/*
//	* 重置节点的显示窗口
//	* */
//	private void pgVideoRestore(String sPeer)
//	{
//		PG_MEMB MembTmp = MembSearch(sPeer);
//		if(MembTmp!=null)
//		{
//			if(MembTmp.sPeer.equals(sPeer))
//			{
//				MembTmp.pLayout.removeView(MembTmp.pView);
//				MembTmp.pView=null;
//				MembTmp.sPeer="";
//				MembTmp.bJoin=false;
//				MembTmp.bVideoSync=false;
//			}
//		}
//	}
//
//	//清理窗口数据和关闭视频
//	private void pgVideoClose(String sPeer)
//	{
//		m_Conf.VideoClose(sPeer);
//		pgVideoRestore(sPeer);
//	}
//	//给所有加入会议的成员发送消息
//	private boolean pgNotifySend()
//	{
//		String sData=m_editText_Notify.getText().toString();
//		if(sData.equals(""))
//		{
//			return false;
//		}
//		m_Conf.NotifySend(sData);
//
//		return true;
//	}
//
//	private boolean m_bSpeechEnable = true;
//
//	//选择自己的声音是否在对端播放
//	private void Speech() {
//		if (!m_Conf.AudioSpeech(m_sMemb,m_bSpeechEnable)) {
//			Log.d("pgRobotClient", "Enable speech failed");
//		} else {
//			m_bSpeechEnable = !m_bSpeechEnable;
//		}
//	}
//
//}
//

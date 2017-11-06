package com.peergine.conference.demo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.peergine.android.conference.pgLibConference;
import com.peergine.plugin.lib.pgLibJNINode;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static com.peergine.android.conference.pgLibConference.*;

/*
*
* TODO: + 说明：
* 如果代码中有该标识，说明在标识处有功能代码待编写，待实现的功能在说明中会简略说明。
* FIXME: + 说明：
* 如果代码中有该标识，说明标识处代码需要修正，甚至代码是错误的，不能工作，需要修复，如何修正会在说明中简略说明。
*
* XXX : + 说明：
* 如果代码中有该标识，说明标识处代码虽然实现了功能，但是实现的方法有待商榷，希望将来能改进，要改进的地方会在说明中简略说明。
*
*
*
* */

/*
 * Updata 2017 02 15 V13
 * 添加定时器的使用示范
 * 修改VideoStart  AudioStart  VideoOpen 的使用时机示范。
 *
 *
 */

public class MainActivity2 extends Activity {

	private String mSGroup = "";
	private String msChair = "";

	private String m_sUser = "";
	private String m_sPass = "";

	private String m_sSvrAddr ="connect.peergine.com:7781";
	private String m_sRelayAddr = "";
	private String m_sVideoParam=
			"(Code){3}(Mode){2}(FrmRate){40}" +
					"(LCode){3}(LMode){3}(LFrmRate){30}" +
					"(Portrait){0}(Rotate){0}(BitRate){300}(CameraNo){"+ Camera.CameraInfo.CAMERA_FACING_FRONT+"}"+
					"(AudioSpeechDisable){0}";
	private String mSmemb = "";

	private pgLibConference mConf = null;
	private pgLibJNINode m_Node = null;

	private int[] ridlaout = {R.id.layoutVideoS1, R.id.layoutVideoS2, R.id.layoutVideoS3};

	private EditText mEditchair =null;
	private Button mBtnstart = null;
	private Button mBtnstop = null;
	private Button mBtnclean = null;
	private Button mBtnlanscan = null;

	private EditText mEdittextNotify =null;
	private Button m_btnNotifySend= null;
	private TextView text_info = null;
	private Button m_btntest=null;
    private Button m_btn_recordstart=null;
    private Button m_btn_recordstop=null;

	private LinearLayout mPreviewLayout =null;
	private SurfaceView mPreview =null;
	private Button m_BtnClearlog = null;
	private String mMode = "";

	//R.id.layoutVideoS0,

	class PG_MEMB{
		String sPeer="";
		Boolean bVideoSync=false;
		Boolean bJoin=false;
		SurfaceView pView=null;
		LinearLayout pLayout=null;
	}

	private static ArrayList<PG_MEMB> mMemberArray = new ArrayList<>();

	private View.OnClickListener mOnclink = new View.OnClickListener() {
		// Control clicked
		public void onClick(View args0) {
			int k=0;
			switch (args0.getId()) {
				case R.id.btn_Start:
					m_bVideoStart = false;

					pgStart();
					mBtnstart.setEnabled(false);
					Log.d("OnClink", "init button");
					break;
				case R.id.btn_stop:
					pgStop();
					mBtnstart.setEnabled(true);
					Log.d("OnClink", "MemberAdd button");
					break;
				case R.id.btn_Clean:
					pgClean();
					mBtnstart.setEnabled(true);
					Log.d("OnClink", "MemberAdd button");
					break;
				case R.id.btn_LanScan:
					mConf.LanScanStart();
					break;

				case R.id.btn_notifysend:
//					//Group_member869384011853858
//					mConf.MessageSend("hellllllo","Group_member869384011853858");
//					//pgNotifySend();
				{

//					String sPath = getSDPath() + "/Video.avi";
//					mConf.VideoRecord("_DEV_" + msChair, sPath);
//					Log.d("OnClink", "MemberAdd button");
//					break;
					mConf.CallSend("hello", msChair,"123");
					break;
				}
				case R.id.button:
//              test Api
				{
					//mConf.SvrRequest("(User){" + mConf.GetNode().omlEncode("_DEV_358180050453651_chairman")
					//		+ "}(Msg){" + mConf.GetNode().omlEncode("hello chairman") + "}");
					//mConf.AudioCtrlVolume(msChair,0,0);
					//mConf.Reset(mSGroup,"Group_member869384011853858");
//                        Date currentTime = new Date();
//                        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
//                        String sDate = formatter.format(currentTime);
//						String sPath = getSDPath()+"/record"+sDate+".avi";
//						mConf.RecordStart(msChair,sPath);
//						mConf.AudioRecordStart(msChair,sPath);

					break;

				}
				case R.id.btn_recordstart:{
					Date currentTime = new Date();
					SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
					String sDate = formatter.format(currentTime);
					String sPath = getSDPath()+"/record"+sDate+".avi";
					if((!mConf.RecordStart(msChair,sPath))){
						Toast.makeText(getApplication(),"录像失败。 已经关闭",Toast.LENGTH_SHORT).show();
						mConf.RecordStop(msChair);
//						mConf.AudioRecordStop(msChair);
					}
					break;
				}
				case R.id.btn_recordstop:{
					mConf.RecordStop(msChair);
					//mConf.AudioRecordStop(msChair);
					break;
				}
				case R.id.btn_clearlog:{
					text_info.setText("");
				}
				default:

					break;
			}
		}
	};
	//定时器例子 超时处理实现
	TimerOut timerOut =new TimerOut() {
		@Override
		public void TimerProc(String sParam) {
			if(m_Node==null) {
				return;
			}

			//中间件oml 格式数据解析示例
			String sAct = m_Node.omlGetContent(sParam,"Act");
			if("DoVideoOpen".equals(sAct)){
				//
				String sPeer = m_Node.omlGetContent(sParam,"Peer");
				if(!sPeer.equals("_DEV_"+m_sUser))
				{
					String sObjSelf="_DEV_"+m_sUser;
					/**
					* Demo 是为了演示方便 在这里实现自动打开视频的功能
					* 所以才做了这个ID大的主动打开视频
					* 实际情况中建议从Join出得到设备列表，或者本地保存列表，用ListView显示，点击某个ID然后开始打开视频
					*/
					if(sObjSelf.compareTo(sPeer)>0)
					{
						Show( " 发起视频请求");

						//TODO 客户使用按钮请求视频更好。
						pgVideoOpen(sPeer);
					}

				}
			}
		}
	};


	void initView(){
		/**
		 * 4个窗口初始化
		 */
		mPreviewLayout = findViewById(R.id.layoutVideoS0);
		for (int aRIDLaout : ridlaout) {
			PG_MEMB oMemb = new PG_MEMB();
			oMemb.pLayout = findViewById(aRIDLaout);
			mMemberArray.add(oMemb);
		}


		/**
		 * 初始化控件
		 */


		mEditchair = (EditText) findViewById(R.id.editText_chair);

		mBtnstart = (Button) findViewById(R.id.btn_Start);
		mBtnstart.setOnClickListener(mOnclink);

		mBtnstop = (Button) findViewById(R.id.btn_stop);
		mBtnstop.setOnClickListener(mOnclink);

		mBtnclean =(Button) findViewById(R.id.btn_Clean);
		mBtnclean.setOnClickListener(mOnclink);

		mBtnlanscan = (Button)findViewById(R.id.btn_LanScan);
		mBtnlanscan.setOnClickListener(mOnclink);

		mEdittextNotify =(EditText)findViewById(R.id.editText_notify);

		m_btnNotifySend=(Button) findViewById(R.id.btn_notifysend);
		m_btnNotifySend.setOnClickListener(mOnclink);

		m_btntest=(Button) findViewById(R.id.button);
		m_btntest.setOnClickListener(mOnclink);

		m_btn_recordstart=(Button) findViewById(R.id.btn_recordstart);
		m_btn_recordstart.setOnClickListener(mOnclink);

		m_btn_recordstop=(Button) findViewById(R.id.btn_recordstop);
		m_btn_recordstop.setOnClickListener(mOnclink);

		m_BtnClearlog = (Button) findViewById(R.id.btn_clearlog);
		m_BtnClearlog.setOnClickListener(mOnclink);
		//显示一些信息
		text_info= (TextView) findViewById(R.id.text_info);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.fragment_main);


		mMemberArray.clear();

		initView();


//		String sConfig_Node = "Type=0;Option=1;MaxPeer=256;MaxGroup=32;MaxObject=512;MaxMCast=512;MaxHandle=256;SKTBufSize0=128;SKTBufSize1=64;SKTBufSize2=256;SKTBufSize3=64";
		Intent intent = getIntent();
		m_sUser = 			intent.getStringExtra("User");
		m_sPass =			intent.getStringExtra("User");
		m_sSvrAddr = 		intent.getStringExtra("SvrAddr");
		m_sRelayAddr = 	intent.getStringExtra("RelayAddr");
		m_sVideoParam = 	intent.getStringExtra("VideoParam");

		int iExpire = 		ParseInt(intent.getStringExtra("Expire"),10);
		int iMaxPeer = 		ParseInt(intent.getStringExtra("MaxPeer"),256);
		int iMaxObject = 	ParseInt(intent.getStringExtra("MaxObject"),512);
		int iMaxMCast = 	ParseInt(intent.getStringExtra("MaxMCast"),512);
		int iMaxHandle = 	ParseInt(intent.getStringExtra("MaxHandle"),256);

		mMode = intent.getStringExtra("Mode");

		if(mConf ==null){
			mConf = new pgLibConference();
			mConf.SetEventListener(m_OnEvent);

			mConf.SetExpire(iExpire);
			PG_NODE_CFG mNodeCfg = new PG_NODE_CFG();

				mNodeCfg.MaxPeer = iMaxPeer;
				mNodeCfg.MaxObject = iMaxObject;
				mNodeCfg.MaxMCast = iMaxMCast;
				mNodeCfg.MaxHandle= iMaxHandle;

			mConf.ConfigNode(mNodeCfg);

		}
		if(!mConf.Initialize(m_sUser,m_sPass,m_sSvrAddr,m_sRelayAddr,m_sVideoParam,this)) {
			Log.d("pgConference", "Init failed");

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Error");
			builder.setMessage("请安装pgPlugin xx.APK 或者检查网络状况!");
			builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}
			});
			builder.show();
			return;
		}

		mPreview = mConf.PreviewCreate(160, 120);
		mPreviewLayout.removeAllViews();
		mPreviewLayout.addView(mPreview);

		m_Node= mConf.GetNode();
		mConf.TimerOutAdd(timerOut);
	}
	@Override
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
				pgStop();
				mConf.Clean();
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

	/**
	 * 设置摄像头旋转角度。
	 * @param iAngle
	 */
	public void SetRotate(int iAngle) {
		pgLibJNINode Node = mConf.GetNode();
		if (Node != null) {
			if (Node.ObjectAdd("_vTemp", "PG_CLASS_Video", "", 0)) {
				Node.ObjectRequest("_vTemp", 2, "(Item){2}(Value){" + iAngle + "}", "");
				Node.ObjectDelete("_vTemp");
			}
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
		return id == R.id.action_settings || super.onOptionsItemSelected(item);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
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
				.equals(Environment.MEDIA_MOUNTED);  //判断sd卡是否存在
		if  (sdCardExist)
		{
			sdDir = Environment.getExternalStorageDirectory();//获取跟目录
		}

		return (sdDir==null)?"":sdDir.toString();

	}
	private void Show(String s){
		String sInfo = text_info.getText()+"\n"+s;
		text_info.setText(sInfo);
	}

	private int iflag=0;

	/**
	 * 设置设备的麦克风的采样率。
	 * 在初始化成功之后，打开音频通话之前调用。
	 * @param iRate: 采样率，单位HZ/秒。有效值：8000, 1600, 32000
	 * 示范利用中间件编写扩展
	 */

	public void SetAudioSampleRate(int iRate) {
		pgLibJNINode Node = mConf.GetNode();
		if (Node != null) {
			// Set microphone sample rate
			if (Node.ObjectAdd("_AudioTemp", "PG_CLASS_Audio", "", 0)) {
				Node.ObjectRequest("_AudioTemp", 2, "(Item){2}(Value){" + iRate + "}", "");
				Node.ObjectDelete("_AudioTemp");
			}
		}
	}

	/**
	 *
	 * @param iVolumeGate 放音门限
	 * @return true 成功，false 失败
	 */
	public boolean SetVolumeGate(int iVolumeGate)
	{
		pgLibJNINode Node = mConf.GetNode();
		if (Node != null) {
			if (Node.ObjectAdd("_aTemp", "PG_CLASS_Audio", "", 0)) {
				String sValue = Node.omlEncode("(TailLen){0}(VolGate){" + iVolumeGate + "}");
				Node.ObjectRequest("_aTemp", 2, "(Item){3}(Value){" + sValue + "}", "");
				Node.ObjectDelete("_aTemp");
				return true;
			}
		}
		return false;
	}

	/**
	 *
	 * 设置杂音抑制参数。在初始化成功之后，打开音频通话之前调用
	 * @param iDebug 是否打开调试信息：1:打开调试，0:关闭调试
	 * @param iDelay 延时的音频帧数
	 * @param iKeep 抑制的音频帧数
	 */
	public void SetAudioSuppress(int iDebug, int iDelay, int iKeep) {
		pgLibJNINode node = mConf.GetNode();
		if (node != null) {
			// Set microphone sample rate
			if (node.ObjectAdd("_AudioTemp", "PG_CLASS_Audio", "", 0)) {
				String sValue = "(Debug){" + iDebug + "}(Delay){" + iDelay + "}(Keep){" + iKeep + "}";
				node.ObjectRequest("_AudioTemp", 2, "(Item){0}(Value){" + node.omlEncode(sValue) + "}", "");
				node.ObjectDelete("_AudioTemp");
			}
		}
	}



	//视频传输的状态
	private void EventVideoFrameStat(String sAct,String sData,String sPeer)
	{
		//Show()

	}
	//服务端下发的通知
	private void EventSvrNotify(String sAct,String sData,String sPeer)
	{
		Show("SvrNotify :"+sData+" : "+sPeer);
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
		if("0".equals(sData))
		{
			Show("已经登录");
			Log.d( "","已经登录");
		}
		else
		{
			Show("登录失败 err = "+sData);
			Log.d( "","登录失败");
		}
	}

	//登出
	private void EventLogout(String sAct,String sData,String sPeer)
	{
		Show( "已经注销"+sData);
	}

	//保存初次连接 ,如果程序崩溃，
//	ArrayList<String> m_PeerLink = new ArrayList<>();
//	boolean PeerLinkSearch(String sPeer){
//		for(int i=0;i<m_PeerLink.size();i++){
//			if(m_PeerLink.get(i).equals(sPeer)){
//				return true;
//			}
//		}
//		return false;
//	}

	//sPeer的离线消息
	private void EventPeerSync(String sAct,String sData,String sPeer)
	{
		// TODO: 2016/11/7 提醒应用程序可以和此节点相互发送消息了
		Show(sPeer+"节点建立连接");
//		if(!PeerLinkSearch(sPeer)){
//			//第一次建立连接，防止己方程序崩溃
//			m_PeerLink.add(sPeer);
//			mConf.MessageSend("SyncFrist:",sPeer);
//		}
		mConf.MessageSend("MessageSend test",sPeer);
		mConf.CallSend("CallSend test",sPeer,"123");

	}
	//sPeer的离线消息
	private void EventPeerOffline(String sAct,String sData,String sPeer)
	{
		// TODO: 2016/11/7 提醒应用程序此节点离线了
		Show( sPeer+"节点离线 sData = "+sData);
	}

	//sPeer的离线消息
	private void EventChairmanSync(String sAct,String sData,String sPeer)
	{
		// TODO: 2016/11/7 提醒应用程序可以和主席发送消息了
		Show( "主席节点建立连接 Act = "+sAct+" : "+sData + " : "+sPeer);
		mConf.Join();
//		if(!PeerLinkSearch(sPeer)){
//			//第一次建立连接，防止己方程序崩溃
//			m_PeerLink.add(sPeer);
//
//		}
		mConf.MessageSend("MessageSend test",sPeer);
		mConf.CallSend("CallSend test",sPeer,"123");
	}
	//sPeer的离线消息
	private void EventChairmanOffline(String sAct,String sData,String sPeer)
	{
		Show( "主席节点离线 sData = "+sPeer);
	}
//-------------------------------------------------------------------------
	//sPeer的离线消息

	private void EventAskJoin(String sAct,String sData,String sPeer)
	{
		// TODO: 2016/11/7 sPeer请求加入会议  MemberAdd表示把他加入会议
		Show( sPeer+"请求加入会议->同意");
		mConf.MemberAdd(sPeer);
	}
	//sPeer的离线消息
	private boolean m_bVideoStart =false;
	private void EventJoin(String sAct,String sData,String sPeer)
	{
		// TODO: 2016/11/7 这里可以获取所有会议成员  可以尝试把sPeer加入会议成员表中
		Show( sPeer+"加入会议");
		String sParam  = "(Act){DoVideoOpen}(Peer){"+sPeer+"}";
		mConf.TimerStart(sParam,1,false);

		mConf.NotifySend(sPeer + " : join ");
		Log.d( "", sPeer+" 加入会议");
	}
	//sPeer的离线消息
	private void EventLeave(String sAct,String sData,String sPeer)
	{
		Show( sPeer+"离开会议");

		Log.d( ""," 离开会议");
	}
//---------------------------------------------------------------------

	//sPeer的离线消息
	private void EventVideoSync(String sAct,String sData,String sPeer)
	{
        // TODO: 2016/11/7 提醒应用程序可以打开这个sPeer的视频了
		Show("视频同步");
	}
	//sPeer的离线消息
	private void EventVideoSyncL(String sAct,String sData,String sPeer)
	{
		// TODO: 2016/11/7 提醒应用可以打开这个sPeer的第二种流
		Log.d( ""," 第二种视频同步");
		Show("第二种视频同步");
	}
	private void EventVideoOpen(String sAct,String sData,String sPeer)
	{
		//收到视频请求
		Show( sPeer + " 请求视频连线->同意");
		//// TODO: 2016/11/7 在这之后回复
		//调用
		pgVideoOpen(sPeer);
	}

	private void EventVideoLost(String sAct, String sData, final String sPeer)
	{
		// TODO: 2016/11/8  对方视频已经丢失 挂断对方视频 并尝试重新打开
		Show(sPeer + " 的视频已经丢失 可以尝试重新连接");
	}


	private void EventVideoClose(String sAct,String sData,String sPeer)
	{
		// TODO: 2016/11/8  通知应用程序视频已经挂断
		Show(sPeer + " 已经挂断视频");
		pgVideoRestore(sPeer);

	}
	private void EventVideoJoin(String sAct,String sData,String sPeer)
	{
		// TODO: 2016/11/8 请求端会收到请求打开视频的结果，打开视频成功除了显示和播放视频外，还有这个事件
		if ("0".equals(sData)) {
			Show(sPeer+":"+ "视频成功打开");
			Log.d("",sPeer + " 成功打开");
		} else {
			Show(sPeer+":"+ "视频成功失败");
			Log.d("", sPeer + " 打开失败");
			pgVideoRestore(sPeer);
		}
	}

	//-------------------------------------------------------------------
	//组消息
	private void EventNotify(String sAct,String sData,String sPeer)
	{
		Show(sPeer + ": sData = "+sData);
	}

	//sPeer的消息处理
	private void EventMessage(String sAct,String sData,String sPeer)
	{
		// TODO: 2016/11/7 处理sPeer发送过来的消息
		Show(sPeer+":"+ sData);
		Log.d("",sPeer+":"+ sData);
	}


	private OnEventListener m_OnEvent = new OnEventListener() {

		@Override
		public void event(String sAct, String sData, final String sPeer) {
			// TODO Auto-generated method stub

			Log.d("ConferenceDemo", "OnEvent: Act=" + sAct + ", Data=" + sData + ", Peer=" + sPeer);
			if (sAct.equals(EVENT_VIDEO_FRAME_STAT)) {
				EventVideoFrameStat(sAct,sData,sPeer);
			}
			else if (sAct.equals(EVENT_LOGIN)) {
				EventLogin(sAct,sData,sPeer);
			}
			else if (sAct.equals(EVENT_LOGOUT)) {
				EventLogout(sAct,sData,sPeer);
			}
			else if (sAct .equals(EVENT_PEER_OFFLINE)) {
				EventPeerOffline(sAct,sData,sPeer);
			}
			else if (sAct.equals(EVENT_PEER_SYNC)) {
				EventPeerSync(sAct,sData,sPeer);
			}
			else if (sAct .equals(EVENT_CHAIRMAN_OFFLINE)) {
				EventChairmanOffline(sAct,sData,sPeer);
			}
			else if (sAct.equals(EVENT_CHAIRMAN_SYNC)) {
				EventChairmanSync(sAct,sData,sPeer);
			}
			else if(sAct.equals(EVENT_ASK_JOIN)) {
				EventAskJoin(sAct,sData,sPeer);
			}
			else if(sAct.equals(EVENT_JOIN)){
				EventJoin(sAct,sData,sPeer);
			}
			else if(sAct.equals(EVENT_LEAVE)){
				EventLeave(sAct,sData,sPeer);
			}
			else if(sAct.equals(EVENT_VIDEO_SYNC)) {
				EventVideoSync(sAct,sData,sPeer);
			}
			else if(sAct.equals(EVENT_VIDEO_SYNC_1)) {
				EventVideoSyncL(sAct,sData,sPeer);
			}
			else if (sAct.equals(EVENT_VIDEO_OPEN)) {
				EventVideoOpen(sAct,sData,sPeer);
			}
			else if(sAct.equals(EVENT_VIDEO_LOST)){
				EventVideoLost(sAct,sData,sPeer);
			}
			else if(sAct.equals(EVENT_VIDEO_CLOSE)){
				EventVideoClose(sAct,sData,sPeer);
			}
			else if (sAct .equals(EVENT_VIDEO_JOIN)) {
				EventVideoJoin(sAct,sData,sPeer);
			}
			else if (sAct .equals(EVENT_VIDEO_CAMERA)) {
				EventVideoCamera(sAct,sData,sPeer);
			}
			else if (sAct .equals( EVENT_VIDEO_RECORD)) {
				EventVideoRecord(sAct,sData,sPeer);
			}
			else if(sAct.equals(EVENT_MESSAGE)) {
				EventMessage(sAct,sData,sPeer);
			}
			else if(sAct.equals(EVENT_CALLSEND_RESULT)) {
				EventCallSend(sAct,sData,sPeer);
			}
			else if(sAct.equals(EVENT_NOTIFY)) {
				EventNotify(sAct,sData,sPeer);
			}
			else if(sAct.equals(EVENT_SVR_NOTIFY)) {
				EventSvrNotify(sAct,sData,sPeer);
			}
			else if(sAct.equals(EVENT_SVR_RELAY)) {
				EventSvrReply(sAct,sData,sPeer);
			}
			else if(sAct.equals(EVENT_SVR_REPLYR_ERROR)) {
				EventSvrReplyError(sAct,sData,sPeer);
			}
			else if(sAct.equals(EVENT_LAN_SCAN_RESULT)){
				EventLanScanResult(sAct,sData,sPeer);
			}
		}
	};

	private void EventCallSend(String sAct, String sData, String sPeer) {
		// CallSend （具有回执的信息） 最终结果
		Show("CallSend 回执 sData = "+sData);
	}

	private void EventVideoRecord(String sAct, String sData, String sPeer) {
		// VideoRecord 视频录制的结果
	}

	private void EventVideoCamera(String sAct, String sData, String sPeer) {
		// VideoCamera 视频拍照的结果
	}
	private void EventLanScanResult(String sAct,String sData,String sPeer){
		Show("Act : LanScanResult  -- sData: "+sData + "  sPeer  : "+sPeer);
	}

	//选择成为主席端的初始化方式
	private void pgChairInit() {

		msChair = mSGroup;
		m_sUser = msChair;
		//m_sUser = "";
		if ("".equals(m_sUser) || "".equals(msChair)) {
			Log.e("Init", "Param Err");
			return;
		}

		if (!mConf.Initialize(mSGroup, msChair, m_sUser, "",
				m_sSvrAddr, "", m_sVideoParam, this)) {
			Log.d("pgConference", "Init failed");
			Alert("Error", "请安装pgPlugin xx.APK 或者检查网络状况!");
			return;
		}
		mPreview = mConf.PreviewCreate(160, 120);
		mPreviewLayout.removeAllViews();
		mPreviewLayout.addView(mPreview);
		mConf.VideoStart(VIDEO_NORMAL);
		mConf.AudioStart();

	}


	//选择成为成员端的初始化方式
	private void pgMembInit() {

		msChair = mSGroup;

		if ("".equals(m_sUser) || "".equals(msChair)) {
			Log.e("Init", "Param Err");
			return;
		}

		if (!mConf.Initialize(mSGroup, msChair, m_sUser, "",
				m_sSvrAddr, "", m_sVideoParam, this)) {
			Log.d("pgConference", "Init failed");
			Alert("Error", "请安装pgPlugin xx.APK 或者检查网络状况!");
			return;
		}
		mPreview = mConf.PreviewCreate(160, 120);
		mPreviewLayout.removeAllViews();
		mPreviewLayout.addView(mPreview);
		mConf.VideoStart(VIDEO_NORMAL);
		mConf.AudioStart();
	}

	private static int ParseInt(String sInt, int iDef) {
		try {
			if ("".equals(sInt)) {
				return 0;
			}
			return Integer.parseInt(sInt);
		} catch (Exception ex) {
			return iDef;
		}
	}

	private void pgStart(){
		msChair = mEditchair.getText().toString().trim();
		if("".equals(msChair)){
			Alert("错误","主席端ID不能为空。");
		}
		String sName = msChair;
		mConf.Start(sName, msChair);
		mConf.VideoStart(VIDEO_NORMAL);
		mConf.AudioStart();
	}

	private void pgStop(){

		for(int i = 0; i< mMemberArray.size(); i++)
		{
			PG_MEMB oMemb= mMemberArray.get(i);
			if(!"".equals(oMemb.sPeer)) {
				pgVideoClose(oMemb.sPeer);
			}
		}
		mConf.AudioStop();
		mConf.VideoStop();
		mConf.Stop();

	}


	//结束会议模块
	private void pgClean()
	{
		pgStop();
		mConf.TimerOutDel(timerOut);
		m_Node=null;

		//mConf.PreviewDestroy();
		mConf.Clean();
		finish();
//		m_PeerLink.clear();
	}

	//搜索成员列表
	PG_MEMB MembSearch(String sPeer) {

		try
		{
			if (sPeer .equals("")) {
				Log.d("","Search can't Search Start");
				return null;
			}
			for (int i = 0; i < mMemberArray.size(); i++) {

				if (mMemberArray.get(i).sPeer .equals(sPeer)|| mMemberArray.get(i).sPeer .equals("")) {
					return mMemberArray.get(i);
				}
			}

		}
		catch (Exception ex) {
			Log.d("","VideoOption. ex=" + ex.toString());
		}
		return null;
	}

	/**
	 * 打开视频时完成窗口和相关数据的改变
	 * @param sPeer 对象ID
	 * @return
	 */
	private boolean pgVideoOpen(String sPeer) {

		PG_MEMB MembTmp = MembSearch(sPeer);
		//没有窗口了
		if(MembTmp==null)
		{
			mConf.VideoReject(sPeer);
			return false;
		}
		if("".equals(MembTmp.sPeer))
		{
			MembTmp.sPeer=sPeer;
		}

		MembTmp.pView= mConf.VideoOpen(sPeer,160,120);
		if(MembTmp.pView!=null)
		{
			MembTmp.pLayout.removeAllViews();
			MembTmp.pLayout.addView(MembTmp.pView);
		}

		return true;

	}

	/**
	 * 重置节点的显示窗口
	 * @param sPeer
	 */
	private void pgVideoRestore(String sPeer)
	{
		PG_MEMB membTmp = MembSearch(sPeer);
		if(membTmp!=null)
		{
			if(membTmp.sPeer.equals(sPeer))
			{
				membTmp.pLayout.removeView(membTmp.pView);
				membTmp.pView=null;
				membTmp.sPeer="";
				membTmp.bJoin=false;
				membTmp.bVideoSync=false;
			}
		}
	}

	//清理窗口数据和关闭视频
	private void pgVideoClose(String sPeer)
	{
		mConf.VideoClose(sPeer);
		pgVideoRestore(sPeer);
	}
	//给所有加入会议的成员发送消息
	private boolean pgNotifySend()
	{
		String sData= mEdittextNotify.getText().toString();
		if("".equals(sData))
		{
			return false;
		}
		mConf.NotifySend(sData);

		return true;
	}

	private boolean m_bSpeechEnable = true;

	/**
	 * 选择自己的声音是否在对端播放
	 */
	private void Speech() {
		if (!mConf.AudioSpeech(mSmemb,m_bSpeechEnable)) {
			Log.d("pgRobotClient", "Enable speech failed");
		} else {
			m_bSpeechEnable = !m_bSpeechEnable;
		}
	}

}


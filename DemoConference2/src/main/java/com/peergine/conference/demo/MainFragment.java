package com.peergine.conference.demo;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.peergine.android.conference.pgLibConference;
import com.peergine.android.conference.pgLibTimer;
import com.peergine.conference.demo.example.SqlParser;
import com.peergine.conference.demo.sqlite.DatabaseHelper;
import com.peergine.plugin.exter.VideoAudioInputExternal;
import com.peergine.plugin.lib.pgLibJNINode;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import me.yokeyword.fragmentation.SupportFragment;

import static android.text.TextUtils.isEmpty;
import static com.peergine.android.conference.pgLibConference.OnEventListener;
import static com.peergine.android.conference.pgLibConference.PG_NODE_CFG;
import static com.peergine.android.conference.pgLibConference.PG_RECORD_NORMAL;
import static com.peergine.android.conference.pgLibConference.PG_RECORD_ONLYVIDEO_HASAUDIO;
import static com.peergine.android.conference.pgLibConference.VIDEO_NORMAL;
import static com.peergine.android.conference.pgLibConference.VIDEO_ONLY_INPUT;
import static com.peergine.android.conference.pgLibConferenceEvent.*;
import static com.peergine.android.conference.pgLibError.PG_ERR_Normal;

/**
 * Updata 2017 02 15 V13
 * 添加定时器的使用示范
 * 修改VideoStart  AudioStart  VideoOpen 的使用时机示范。
 */

public class MainFragment extends SupportFragment {

    private String msChair = "";

    private String m_sUser = "";
    private String m_sPass = "";

    private String m_sSvrAddr = "connect.peergine.com:7781";
    private String m_sRelayAddr = "";
    private String m_sInitParam = "";

    private String m_sPrewParam = "(Code){3}(Mode){2}(FrmRate){40}" + "(Portrait){0}(Rotate){0}(BitRate){300}(CameraNo){" + Camera.CameraInfo.CAMERA_FACING_FRONT + "}";
    private String m_sVideoParam = "(Code){3}(Mode){2}(FrmRate){40}";
    private String m_sVideoParamLarge ="(Code){3}(Mode){2}(FrmRate){40}";


    private int[] ridlaout = {R.id.layoutVideoS1, R.id.layoutVideoS2, R.id.layoutVideoS3};

    private EditText mEditchair = null;

    private EditText mEdittextNotify = null;

    private TextView text_info = null;


    private LinearLayout mPreviewLayout = null;
    private SurfaceView mPreview = null;
    private int REQ_MSG = 10;


    // view 放大
    private View.OnClickListener layoutOnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int iWndID = v.getId();

            SurfaceView videoview = (SurfaceView) ((LinearLayout)v).getChildAt(0);

            ((LinearLayout)v).removeAllViews();

            startForResult(FullScreenFragment.newInstance(videoview,iWndID),REQ_MSG);
        }
    };
    private String sMode = "";
    private String sPath = "";

    //R.id.layoutVideoS0,

    class MEMBER {
        String sPeer = "";
        Boolean bVideoSync = false;
        Boolean bJoin = false;
        SurfaceView pView = null;
        LinearLayout pLayout = null;
    }

    private static ArrayList<MEMBER> mListMemberS = new ArrayList<>();




    public static MainFragment newInstance(String sUser, String sPass, String sSvrAddr, String sRelayAddr,
                                           String sInitParam, String sVideoParam, String sExpire ,String sMode) {
        MainFragment fragment = new MainFragment();
        Bundle args = new Bundle();
        args.putString("User", sUser);
        args.putString("SvrAddr", sSvrAddr);
        args.putString("InitParam", sInitParam);
        args.putString("VideoParam", sVideoParam);
        args.putString("Mode", sMode);
        fragment.setArguments(args);
        return fragment;
    }

    void initView(final View view) {
        /**
         * 4个窗口初始化
         */
        mPreviewLayout = view.findViewById(R.id.layoutVideoS0);
        mPreviewLayout.setOnClickListener(layoutOnClick);
        for (int aRIDLaout : ridlaout) {
            MEMBER oMemb = new MEMBER();
            oMemb.pLayout = view.findViewById(aRIDLaout);
            oMemb.pLayout.setOnClickListener(layoutOnClick);
            mListMemberS.add(oMemb);
        }


        /**
         * 初始化控件
         */


        mEditchair = (EditText) view.findViewById(R.id.editText_chair);

        view.findViewById(R.id.btn_Start).setOnClickListener(mOnclink);
        view.findViewById(R.id.btn_stop).setOnClickListener(mOnclink);
        view.findViewById(R.id.btn_Clean).setOnClickListener(mOnclink);
        view.findViewById(R.id.btn_LanScan).setOnClickListener(mOnclink);

        mEdittextNotify = (EditText) view.findViewById(R.id.editText_notify);

        view.findViewById(R.id.btn_notifysend).setOnClickListener(mOnclink);
        view.findViewById(R.id.btn_msg).setOnClickListener(mOnclink);
        view.findViewById(R.id.btn_svr_request).setOnClickListener(mOnclink);

        view.findViewById(R.id.btn_recordstart).setOnClickListener(mOnclink);
        view.findViewById(R.id.btn_recordstop).setOnClickListener(mOnclink);
        view.findViewById(R.id.btn_test).setOnClickListener(mOnclink);

        view.findViewById(R.id.btn_file_put).setOnClickListener(mOnclink);
        view.findViewById(R.id.btn_file_get).setOnClickListener(mOnclink);
        view.findViewById(R.id.btn_clearlog).setOnClickListener(mOnclink);
        //显示一些信息
        text_info = (TextView) view.findViewById(R.id.text_info);

        String chairman_id = SqlParser.readSqlChairmanID(getActivity().getApplicationContext());
        if(!isEmpty(chairman_id)){
            mEditchair.setText(chairman_id);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        mListMemberS.clear();
        initView(view);

        createTestDir();

        Bundle args = getArguments();
        m_sUser = args.getString("User");
        m_sPass = args.getString("User");
        m_sSvrAddr = args.getString("SvrAddr");
        m_sRelayAddr = args.getString("RelayAddr");
        m_sInitParam = args.getString("InitParam");
        m_sVideoParam = args.getString("VideoParam");

        int iExpire = ParseInt(args.getString("Expire"), 10);
        sMode = args.getString("Mode");
       

        if("1".equals(sMode)){
            m_sVideoParam += "(VideoInExternal){1}";
        }

        pop_count = 0;
        return view;
    }

    private void createTestDir() {
        File file = new File("/sdcard/test");
        if(!file.exists()){
            file.mkdirs();
            showInfo("创建 测试文件夹成功 /sdcard/test");
        }else{
            showInfo("测试文件夹已经存在 /sdcard/test");
        }
    }

    @Override
    public void onDestroyView() {
        if("1".equals(sMode)) {
            external.VideoInputExternalDisable();
        }
        pgStop();
        mConf.Clean();
        pop_count = 0;
        super.onDestroyView();

    }


    @Override
    public boolean onBackPressedSupport() {
        // 对于 4个类别的主Fragment内的回退back逻辑,已经在其onBackPressedSupport里各自处理了
        return true;
    }


    @Override
    public void onFragmentResult(int requestCode, int resultCode, Bundle data) {
        super.onFragmentResult(requestCode, resultCode, data);
        if (requestCode == REQ_MSG && resultCode == RESULT_OK) {

            int iWndID = data.getInt("WndID");
            LinearLayout linearLayout = this.getView().findViewById(iWndID);

            if(R.id.layoutVideoS0 == iWndID){
                if("1".equals(sMode)){
                    external.VideoInputExternalDisable();
                    external.VideoInputExternalEnable();
                }else{
                    linearLayout.addView(mPreview);
                }

            }
            else {
                for (MEMBER oMemb : mListMemberS) {
                    if (oMemb.pLayout.equals(linearLayout)) {
                        if (oMemb.pView!=null){
                            linearLayout.addView(oMemb.pView);
                        }
                    }
                }
            }
        }
    }

    private DialogInterface.OnClickListener m_DlgClick = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == AlertDialog.BUTTON_POSITIVE) {
                pgStop();
                mConf.Clean();
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        }
    };

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
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(sTitle);
        builder.setMessage(sMsg);
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    public String getSDCardDir() {
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState()
                .equals(Environment.MEDIA_MOUNTED);  //判断sd卡是否存在
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();//获取跟目录
        }

        return (sdDir == null) ? "" : sdDir.toString();

    }

    private Toast toast = null;
    private void showInfo(String s) {
        if(toast == null){
            toast = Toast.makeText(getContext(),s,Toast.LENGTH_SHORT);
        }else {
            toast.setText(s);
        }
        toast.show();
    }




    int pop_count = 0;
    private final View.OnClickListener mOnclink = new View.OnClickListener() {
        @Override
        public void onClick(View args0) {
            int k = 0;
            boolean bErr;
            int iErr;
            switch (args0.getId()) {
                case R.id.btn_Start:
                    m_bVideoStart = false;
                    pgStart();
                    Log.d("OnClink", "init button");
                    break;
                case R.id.btn_stop:
                    pgStop();
                    Log.d("OnClink", "MemberAdd button");
                    break;
                case R.id.btn_Clean:
                    pgClean();
                    if(getFragmentManager().getBackStackEntryCount() > 1){
                        if(pop_count>=1)
                        {
                            showInfo("Clean 已经按下，正在等待退出。请稍等。");
                            break;
                        }

                        pop();
                        pop_count ++;
                    }

                    Log.d("OnClink", "MemberAdd button");
                    break;
                case R.id.btn_LanScan:
                    bErr = mConf.LanScanStart();
                    if(!bErr){
                        showInfo(" LanScanStart return false");
                    }
                    break;
                case R.id.btn_notifysend: {
                    String sMsg = mEdittextNotify.getText().toString().trim();
                    bErr = mConf.NotifySend(sMsg);
                    if(!bErr){
                        showInfo(" NotifySend return false");
                    }
                    break;
                }
                case R.id.btn_msg: {
                    String sMsg = mEdittextNotify.getText().toString().trim();
                    bErr =  mConf.MessageSend(sMsg, msChair);
                    if(!bErr){
                        showInfo(" MessageSend return false");
                    }
                    break;

                } case R.id.btn_svr_request: {
                    String sMsg = mEdittextNotify.getText().toString().trim();
                    if(!mConf.SvrRequest(sMsg)){
                        showInfo(" SvrRequest return false");
                    };
                    break;

                }
                case R.id.btn_recordstart: {
                    pgRecordStartNew();
                    break;
                }
                case R.id.btn_recordstop: {
                    pgRecordStopNew();
                    break;
                }
                case R.id.btn_test: {
                    test();
                    break;

                }case R.id.btn_file_put: {

                    iErr = mConf.FilePutRequest(msChair,m_sUser,"/sdcard/test/test.avi","");
                    if(iErr >PG_ERR_Normal){
                        showInfo(" FilePutRequest return false");
                    }
                    break;

                }case R.id.btn_file_get: {
                    Date currentTime = new Date();
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
                    String sDate = formatter.format(currentTime);
                    iErr = mConf.FileGetRequest(msChair,m_sUser,"/sdcard/test/GetFile_" + sDate + ".avi","");
                    if(iErr >PG_ERR_Normal){
                        showInfo(" FilePutRequest return false");
                    }
                    break;

                }
                case R.id.btn_clearlog: {
                    text_info.setText("");
                }
                default:

                    break;
            }
        }
    };

    int m_iVelue = 0;
    private void test() {
        boolean bReport = m_iVelue > 0;

        int iErr = mConf.PeerGetInfo(msChair,bReport);
        if (iErr> PG_ERR_Normal){
            showInfo("PeerGetInfo iErr = " + iErr);
        }
        m_iVelue ++;
        if(m_iVelue > 1){
            m_iVelue = 0;
        }
    }
    private void testAudioMuteInput() {

        int iErr = mConf.AudioMuteInput(m_iVelue);
        showInfo("AudioMuteInput " + m_iVelue + " , Err = " + iErr);
        m_iVelue ++;
        if(m_iVelue > 1){
            m_iVelue = 0;
        }
    }
}


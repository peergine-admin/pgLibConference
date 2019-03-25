package com.peergine.conference.demo2;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.peergine.conference.demo2.example.ConfNameList;
import com.peergine.conference.demo2.example.Conference;
import com.peergine.conference.demo2.example.LayoutMange;
import com.peergine.conference.demo2.example.SqlParser;
import com.peergine.conference.democonference2.R;

import java.io.File;

import me.yokeyword.fragmentation.SupportFragment;

import static android.text.TextUtils.isEmpty;
import static com.peergine.android.conference.pgLibConference2._ParseInt;
import static com.peergine.android.conference.pgLibError.PG_ERR_Normal;
import static com.peergine.android.conference.pgLibError.pgLibErr2Str;

/**
 * Updata 2017 02 15 V13
 * 添加定时器的使用示范
 * 修改VideoStart  AudioStart  VideoOpen 的使用时机示范。
 */

public class MainFragment extends SupportFragment {

    private boolean isInputExternal = false;
//    private String msChair = "";

    private String m_sUser = "";
    private String m_sPass = "";

    private String m_sSvrAddr = "connect.peergine.com:7781";
    private String m_sRelayAddr = "";
    private String m_sInitParam = "";

    private final Conference conference = new Conference();
    private final LayoutMange layoutMange = new LayoutMange();

    private int[] linearLayouts = {R.id.layoutVideoS0, R.id.layoutVideoS1, R.id.layoutVideoS2, R.id.layoutVideoS3};

    private EditText mEdittextNotify = null;

    private TextView text_info = null;
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

        for(int i = 0 ; i < linearLayouts.length ; i ++){
            LinearLayout linearLayout = (LinearLayout) view.findViewById(linearLayouts[i]);
            layoutMange.Add(linearLayout);
            //linearLayout.setOnClickListener(layoutOnClick);
        }

        /**
         * 初始化控件
         */
        ViewGroup menuchar1 = view.findViewById(R.id.menu_chair_1);

        menuchar1.findViewById(R.id.btn_item_start).setOnClickListener(mOnclinkMenu);
        menuchar1.findViewById(R.id.btn_item_stop).setOnClickListener(mOnclinkMenu);
        menuchar1.findViewById(R.id.btn_item_recordstart).setOnClickListener(mOnclinkMenu);
        menuchar1.findViewById(R.id.btn_item_recordstop).setOnClickListener(mOnclinkMenu);
        menuchar1.findViewById(R.id.btn_item_test).setOnClickListener(mOnclinkMenu);

        ViewGroup menuchar2 = view.findViewById(R.id.menu_chair_2);
        menuchar2.findViewById(R.id.btn_item_start).setOnClickListener(mOnclinkMenu);
        menuchar2.findViewById(R.id.btn_item_stop).setOnClickListener(mOnclinkMenu);
        menuchar2.findViewById(R.id.btn_item_recordstart).setOnClickListener(mOnclinkMenu);
        menuchar2.findViewById(R.id.btn_item_recordstop).setOnClickListener(mOnclinkMenu);
        menuchar2.findViewById(R.id.btn_item_test).setOnClickListener(mOnclinkMenu);

        view.findViewById(R.id.btn_Clean).setOnClickListener(mOnclink);

        mEdittextNotify = (EditText) view.findViewById(R.id.editText_notify);

        view.findViewById(R.id.btn_msg).setOnClickListener(mOnclink);
        view.findViewById(R.id.btn_svr_request).setOnClickListener(mOnclink);



        //显示一些信息
        text_info = (TextView) view.findViewById(R.id.text_info);

        String chairman_id = SqlParser.readSqlChairmanID(getActivity().getApplicationContext());
        if(!isEmpty(chairman_id)){
        ((EditText)(view.findViewById(R.id.menu_chair_1).findViewById(R.id.editText_chair))).setText(chairman_id);
        ((EditText)(view.findViewById(R.id.menu_chair_2).findViewById(R.id.editText_chair))).setText(chairman_id);

//            mEditchair.setText(chairman_id);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        initView(view);

        createTestDir();

        Bundle args = getArguments();
        assert args != null;
        m_sUser = args.getString("User");
        m_sPass = args.getString("User");
        m_sSvrAddr = args.getString("SvrAddr");
        m_sRelayAddr = args.getString("RelayAddr");
        m_sInitParam = args.getString("InitParam");

        int iExpire = _ParseInt(args.getString("Expire"), 10);
        sMode = args.getString("Mode");


        if("1".equals(sMode)){
            isInputExternal = true;
        }

        int iErr = conference.Initialize(m_sUser,m_sSvrAddr,getContext(),isInputExternal,layoutMange);
        if(iErr > PG_ERR_Normal){
            conference.showAlert("请安装pgPlugin xx.APK 或者检查网络状况!");
        }
        pop_count = 0;
        return view;
    }

    private void createTestDir() {
        File file = new File("/sdcard/test");
        if(!file.exists()){
            file.mkdirs();
            Log.d("ConfernceDemo 2","创建 测试文件夹成功 /sdcard/test");
        }else{
            showInfo("测试文件夹已经存在 /sdcard/test");
        }
    }

    private Toast toast = null;
    public void showInfo(String s) {

        if(toast == null ){
            toast = Toast.makeText(getContext(),s,Toast.LENGTH_SHORT);
        }else {
            toast.setText(s);
        }
        toast.show();
    }

    @Override
    public void onDestroyView() {

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

//            if(R.id.layoutVideoS0 == iWndID){
//                if("1".equals(sMode)){
//                    external.VideoInputExternalDisable();
//                    external.VideoInputExternalEnable();
//                }else{
//                    linearLayout.addView(mPreview);
//                }
//
//            }
//            else {
//                for (MEMBER oMemb : mListMemberS) {
//                    if (oMemb.pLayout.equals(linearLayout)) {
//                        if (oMemb.pView!=null){
//                            linearLayout.addView(oMemb.pView);
//                        }
//                    }
//                }
//            }
        }
    }

    private DialogInterface.OnClickListener m_DlgClick = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == AlertDialog.BUTTON_POSITIVE) {

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

    private final View.OnClickListener mOnclinkMenu = new View.OnClickListener() {
        @Override
        public void onClick(View args0) {
            int k = 0;
            boolean bErr;
            int iErr;
            String sChair = "";
            switch (args0.getId()) {
                case R.id.btn_item_start:
                    sChair = ((EditText)((LinearLayout)(args0.getParent())).findViewById(R.id.editText_chair)).getText().toString().trim();
                    // Demo 使用主席名作为 会议名称
                    iErr = conference.pgStart(sChair, sChair);
                    if (iErr > PG_ERR_Normal) {
                        showInfo("创建会议失败。" + pgLibErr2Str(iErr));
                        return;
                    }


                    Log.d("OnClink", "init button");
                    break;
                case R.id.btn_item_stop:
                    sChair = ((EditText)((LinearLayout)(args0.getParent())).findViewById(R.id.editText_chair)).getText().toString().trim();
//                    sChair = mEditchair.getText().toString().trim();

                    conference.pgStop(sChair);
                    Log.d("OnClink", "MemberAdd button");
                    break;
                case R.id.btn_item_recordstart: {
                    sChair = ((EditText)((LinearLayout)(args0.getParent().getParent())).findViewById(R.id.editText_chair)).getText().toString().trim();
                    showInfo("测试录制主席端视频");
                    conference.pgRecordStartNew(sChair,sChair);
                    break;
                }
                case R.id.btn_item_recordstop: {
                    sChair = ((EditText)((LinearLayout)(args0.getParent().getParent())).findViewById(R.id.editText_chair)).getText().toString().trim();
//                    showInfo("测试录制主席端视频");
                    conference.pgRecordStopNew(sChair,sChair);
                    break;
                }case R.id.btn_item_test : {
                    sChair = ((EditText)((LinearLayout)(args0.getParent().getParent())).findViewById(R.id.editText_chair)).getText().toString().trim();
                    conference.pgTest(sChair,sChair);
                    break;
                }
                default:
                    break;
            }
        }
    };

    int pop_count = 0;
    private final View.OnClickListener mOnclink = new View.OnClickListener() {
        @Override
        public void onClick(View args0) {
            int k = 0;
            boolean bErr;
            int iErr;
            switch (args0.getId()) {
//                case R.id.btn_Start:
//                    String sChair = mEditchair.getText().toString().trim();
//
//                    iErr = conference.pgStart(sChair,sChair);
//                    if(iErr > PG_ERR_Normal){
//                        showInfo("创建会议失败。" + pgLibErr2Str(iErr));
//                        return;
//                    }
//
//
//                    Log.d("OnClink", "init button");
//                    break;
//                case R.id.btn_stop:
//                    sChair = mEditchair.getText().toString().trim();
//
//                    conference.pgStop(sChair);
//                    Log.d("OnClink", "MemberAdd button");
//                    break;
                case R.id.btn_Clean:

                    conference.Clean();
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
//                case R.id.btn_LanScan:
//                    bErr = mConf.LanScanStart();
//                    if(!bErr){
//                        showInfo(" LanScanStart return false");
//                    }
//                    break;
//                case R.id.btn_notifysend: {
//                    String sMsg = mEdittextNotify.getText().toString().trim();
//                    bErr = conference.m_Conf2.NotifySend(sMsg);
//                    if(!bErr){
//                        showInfo(" NotifySend return false");
//                    }
//                    break;
//                }
                case R.id.btn_msg: {
                    String sMsg = mEdittextNotify.getText().toString().trim();
                    conference.MessageSend(sMsg);
                    break;

                } case R.id.btn_svr_request: {
                    String sMsg = mEdittextNotify.getText().toString().trim();
                    iErr = conference.m_Conf2.SvrRequest(sMsg);
                    if(iErr > PG_ERR_Normal){
                        showInfo(" SvrRequest iErr = " + pgLibErr2Str(iErr));
                    };
                    break;

                }

//                case R.id.btn_test: {
//                    test();
//                    break;
//
//                }case R.id.btn_file_put: {
//
//                    iErr = mConf.FilePutRequest(msChair,m_sUser,"/sdcard/test/test.avi","");
//                    if(iErr >PG_ERR_Normal){
//                        showInfo(" FilePutRequest return false");
//                    }
//                    break;
//
//                }case R.id.btn_file_get: {
//                    Date currentTime = new Date();
//                    SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
//                    String sDate = formatter.format(currentTime);
//                    iErr = mConf.FileGetRequest(msChair,m_sUser,"/sdcard/test/GetFile_" + sDate + ".avi","");
//                    if(iErr >PG_ERR_Normal){
//                        showInfo(" FilePutRequest return false");
//                    }
//                    break;
//
//                }
//                case R.id.btn_clearlog: {
//                    text_info.setText("");
//                }
                default:

                    break;
            }
        }
    };
}


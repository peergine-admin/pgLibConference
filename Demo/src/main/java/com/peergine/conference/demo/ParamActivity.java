package com.peergine.conference.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class ParamActivity extends Activity {

    EditText m_edit_user = null;
    EditText m_edit_pass = null;
    EditText m_edit_svraddr = null;
    EditText m_edit_relayaddr = null;
    EditText m_edit_videoparam = null;


    EditText m_edit_Expire = null;
    EditText m_edit_MaxPeer = null;
    EditText m_edit_MaxObject = null;
    EditText m_edit_MaxMCast = null;
    EditText m_edit_MaxHandle = null;


    Button m_btn_Init = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_param);
        m_edit_user =       (EditText) findViewById(R.id.editText_user);
        m_edit_pass =       (EditText) findViewById(R.id.editText_pass);
        m_edit_svraddr =    (EditText) findViewById(R.id.editText_svraddr);
        m_edit_relayaddr =  (EditText) findViewById(R.id.editText_relay);
        m_edit_videoparam = (EditText) findViewById(R.id.editText_videoparam);


        m_edit_Expire =     (EditText) findViewById(R.id.editText_expire);
        m_edit_MaxPeer =    (EditText) findViewById(R.id.editText_MaxPeer);
        m_edit_MaxObject =  (EditText) findViewById(R.id.editText_MaxObject);
        m_edit_MaxMCast =   (EditText) findViewById(R.id.editText_MaxMCast);
        m_edit_MaxHandle =  (EditText) findViewById(R.id.editText_MaxHandle);

        m_btn_Init = (Button) findViewById(R.id.btn_init);
        m_btn_Init.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()){
                    case R.id.btn_init:{
                        String sUser = m_edit_user.getText().toString().trim();
                        String sPass = m_edit_pass.getText().toString().trim();
                        String sSvrAddr = m_edit_svraddr.getText().toString().trim();
                        String sRelayAddr = m_edit_relayaddr.getText().toString().trim();
                        String sVideoParam = m_edit_videoparam.getText().toString().trim();
                        if(sUser.equals("")||sSvrAddr.equals("")||sVideoParam.equals("")){
                            Toast.makeText(getApplicationContext(),"部分参数为空。请检查。",Toast.LENGTH_SHORT).show();
                            break;
                        }

                        String sExpire =     m_edit_Expire.getText().toString().trim();
                        String sMaxPeer =    m_edit_MaxPeer.getText().toString().trim();
                        String sMaxObject =     m_edit_MaxObject.getText().toString().trim();
                        String sMaxMCast =    m_edit_MaxMCast.getText().toString().trim();
                        String sMaxHandle =     m_edit_MaxHandle.getText().toString().trim();

                        Intent intent = new Intent();

                        intent.putExtra("User",sUser);
                        intent.putExtra("Pass",sPass);
                        intent.putExtra("SvrAddr",sSvrAddr);
                        intent.putExtra("RelayAddr",sRelayAddr);
                        intent.putExtra("VideoParam",sVideoParam);
                        intent.putExtra("Expire",sExpire);
                        intent.putExtra("MaxPeer",sMaxPeer);
                        intent.putExtra("MaxObject",sMaxObject);
                        intent.putExtra("MaxMCast",sMaxMCast);
                        intent.putExtra("MaxHandle",sMaxHandle);

                        intent.setClass(ParamActivity.this,MainActivity.class);
                        startActivity(intent);

                        break;
                    }
                    default:break;
                }
            }
        });
    }
}

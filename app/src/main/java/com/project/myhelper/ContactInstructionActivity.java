package com.project.myhelper;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import androidx.appcompat.widget.Toolbar;

public class ContactInstructionActivity extends AppCompatActivity {

    Toolbar toolbar;
    ImageView imageView;
    TextView txt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_instruction);

        txt=findViewById(R.id.textview_for_contact);
        txt.setText("MobiFix <yourLoginPassword> getContact <contactName>.");

        toolbar=findViewById(R.id.toolbar);
        toolbar.setTitle("ACCESS CONTACTS");
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(),MainActivity.class));
            }
        });

        imageView=findViewById(R.id.contact_instruction_image);
    }
}

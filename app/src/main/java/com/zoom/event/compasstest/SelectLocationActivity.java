package com.zoom.event.compasstest;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class SelectLocationActivity extends AppCompatActivity {


    Spinner locspn;
    Button locbtn;

    JSONObject mainJobj;
    JSONArray mainJarray;

    String locjson="";
    ArrayList<Locationdata> locarray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_location);

        locjson = loadJSONFromAsset(SelectLocationActivity.this);



        locspn = findViewById(R.id.locspin);
        locbtn = findViewById(R.id.locbtn);

        try {
            mainJobj = new JSONObject(locjson);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        System.out.println("Main Object : "+mainJobj.toString());

        locbtn.setOnClickListener(new View.OnClickListener() {



            @Override
            public void onClick(View v) {
                String spsel = locspn.getSelectedItem().toString();

                Toast.makeText(getApplicationContext(),"Selected Item is : "+spsel,Toast.LENGTH_LONG).show();

                if (spsel.matches("Select Location"))
                {
                    Toast.makeText(getApplicationContext(),"Please Select Location!",Toast.LENGTH_LONG).show();
                }else if (spsel.matches("loca1"))
                {
                    locarray =  getLocfromJSON("loca1");
                    Intent inactivity = new Intent(SelectLocationActivity.this,MainActivity.class);
                    inactivity.putExtra("loc",locarray);
                    startActivity(inactivity);

                }else if (spsel.matches("Fire Exit"))
                {
                    locarray =  getLocfromJSON("Fire Exit");
                    Intent inactivity = new Intent(SelectLocationActivity.this,MainActivity.class);
                    inactivity.putExtra("loc",locarray);
                    startActivity(inactivity);

                }else if (spsel.matches("8.185 Lab"))
                {
                    locarray =  getLocfromJSON("8.185 Lab");
                    Intent inactivity = new Intent(SelectLocationActivity.this,MainActivity.class);
                    inactivity.putExtra("loc",locarray);
                    startActivity(inactivity);
                }
            }
        });

    }


    public String loadJSONFromAsset(Context context) {
        String json = null;
        try {
            InputStream is = context.getAssets().open("mylocations.json");

            int size = is.available();

            byte[] buffer = new byte[size];

            is.read(buffer);

            is.close();

            json = new String(buffer, "UTF-8");


        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;

    }


    public ArrayList<Locationdata> getLocfromJSON(String loc)
    {
        ArrayList<Locationdata> oneLoc = new ArrayList<>();


        try {
            mainJarray =  mainJobj.getJSONArray("locations");
            System.out.println("MainArray "+mainJarray.toString());

            for (int i = 0;i<mainJarray.length();i++)
            {
                JSONObject newobj = mainJarray.getJSONObject(i);


                if (newobj.has(loc))
                {
                   // System.out.println("LocDATA "+newobj.getJSONArray(loc));
                    JSONArray innerarray =  newobj.getJSONArray(loc);
                    System.out.println("LocDATA "+innerarray);

                    for (int k=0;k<innerarray.length();k++)
                    {
                        JSONObject innerobj =  innerarray.getJSONObject(k);
                        oneLoc.add(new Locationdata(innerobj.getDouble("latitude"),innerobj.getDouble("longitude")));
                    }

                }
            }


        } catch (JSONException e) {
            e.printStackTrace();
        }

        return oneLoc;
    }
}

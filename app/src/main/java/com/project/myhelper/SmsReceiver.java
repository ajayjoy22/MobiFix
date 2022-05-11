package com.project.myhelper;

import android.annotation.TargetApi;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;
import android.app.ActivityManager;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;


public class SmsReceiver extends BroadcastReceiver {

    public DatabaseHelper mydb;

    GPSTracker gps;
    double latitute,longitude;

    public static final String pdu_type = "pdus";
    public String secret_code,lockPin,email;
    public boolean contact_switchOn,location_switchOn,ringProfile_switchOn,alarm_switchOn,lockMobileScreen_switchOn;

    FirebaseAuth firebaseAuth;

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onReceive(final Context context, Intent intent) {
        Toast.makeText(context, "Message received", Toast.LENGTH_SHORT).show();
        Log.v("aa", "Something received");
        if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
            Log.v("aa", "Something received:android.provider.Telephony.SMS_RECEIVED");
            Bundle bundle = intent.getExtras();
            SmsMessage[] msgs;
            String strMessage = "";
            String format = bundle.getString("format");
            Object[] pdus = (Object[]) bundle.get(pdu_type);

            SharedPreferences pref=context.getSharedPreferences("UserData",Context.MODE_PRIVATE);
            secret_code=pref.getString("Password","");
            lockPin=pref.getString("LockPin","");
            email=pref.getString("Email","");

            SharedPreferences pref1= PreferenceManager.getDefaultSharedPreferences(context);
            contact_switchOn=pref1.getBoolean("access_contact",true);
            location_switchOn=pref1.getBoolean("access_location",true);
            ringProfile_switchOn=pref1.getBoolean("access_profile",true);
            alarm_switchOn=pref1.getBoolean("access_alarm",true);
            lockMobileScreen_switchOn=pref1.getBoolean("access_lock",true);

            mydb=new DatabaseHelper(context);

            firebaseAuth=FirebaseAuth.getInstance();


           if(firebaseAuth.getCurrentUser()!=null) {
               if (pdus != null) {
                   boolean isVersionM = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M);
                   msgs = new SmsMessage[pdus.length];
                   for (int i = 0; i < msgs.length; i++) {
                       if (isVersionM) {
                           msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i], format);
                       } else {
                           msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                       }
                       strMessage += "SMS from " + msgs[i].getOriginatingAddress();

                       strMessage += " :" + msgs[i].getMessageBody() + "\n";
                       final String number = msgs[i].getOriginatingAddress();
                       String body = msgs[i].getMessageBody();

                       ArrayList arrayList = mydb.getAllBlockedContacts();
                       for (int j = 0; j < arrayList.size(); j++) {
                           if (arrayList.get(j).equals(number)) {
                               return;
                           }
                       }

                       String[] arr = body.split(" ", 5);
                       int msgLength = arr.length;

                       SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm");
                       String currentDateAndTime = sdf.format(new Date());

                       if (msgLength == 1 && arr[0].equalsIgnoreCase("mobifix")) {

                           mydb.insertIntoMessages(number, body, currentDateAndTime);
                           String text = "Welcome Sir! How can i help you ? \n \n Send  message from one of the following. \n 1. MobiFix Forgot login password \n 2. MobiFix Help";
                           sendMessage(number, text);
                           mydb.insertIntoMessages("reply", text, currentDateAndTime);

                       } else if (arr[0].equalsIgnoreCase("1") && arr[1].equalsIgnoreCase("forgot") && arr[2].equalsIgnoreCase("login") && arr[3].equalsIgnoreCase("password")) {

                           ConnectivityManager ConnectionManager = (ConnectivityManager) context.getSystemService(context.CONNECTIVITY_SERVICE);
                           NetworkInfo networkInfo = ConnectionManager.getActiveNetworkInfo();
                           if (networkInfo != null && networkInfo.isConnected()) {

                               firebaseAuth.sendPasswordResetEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
                                   @Override
                                   public void onComplete(@NonNull Task<Void> task) {

                                       if (task.isSuccessful()) {
                                           sendMessage(number, "password link send to your email");
                                       }
                                   }
                               });
                           } else {
                               sendMessage(number, "No internet available");
                           }
                       } else if (msgLength == 2 && arr[0].equalsIgnoreCase("mobifix") && arr[1].equalsIgnoreCase("help")) {

                           mydb.insertIntoMessages(number, body, currentDateAndTime);
                           String text = "For contacts : " +
                                   "MobiFix <loginPassword> getContact <contactName> \n \n" +
                                   "For Location : " +
                                   "MobiFix <loginPassword> getLocation \n \n" +
                                   "For change Profile : " +
                                   "MobiFix <loginPassword> ChangeProfile \n \n" +
                                   "For Lock Mobile : " +
                                   "MobiFix <loginPassword> setScreenLock\n";
                           sendMultiMessage(number, text);
                           mydb.insertIntoMessages("reply", text, currentDateAndTime);
                       } else if (msgLength > 2 && arr[0].equalsIgnoreCase("mobifix") && arr[1].equals(secret_code)) {

                           if (arr[2].equalsIgnoreCase("getcontact") && contact_switchOn) {

                               String name = "";

                               for (int j = 3; ; j++) {
                                   try {
                                       name += arr[j];
                                   } catch (Exception e) {
                                       mydb.insertIntoMessages(number, body, currentDateAndTime);
                                       String contact_number = getContact(name, context);
                                       sendMessage(number, name + " : " + contact_number);
                                       mydb.insertIntoMessages("reply", name + " : " + contact_number, currentDateAndTime);
                                       break;
                                   }
                                   name += " ";
                               }
                           } else if (arr[2].equalsIgnoreCase("GetLocation") && location_switchOn) {

                               try {
                                   int l = arr[3].length();
                               } catch (Exception e) {
                                   gps = new GPSTracker(context);
                                   if (gps.canGetLocation()) {
                                       latitute = gps.getLatitude();
                                       longitude = gps.getLongitude();
                                   } else {
                                       gps.showSettingsAlert();
                                   }
                                   ConnectivityManager ConnectionManager = (ConnectivityManager) context.getSystemService(context.CONNECTIVITY_SERVICE);
                                   NetworkInfo networkInfo = ConnectionManager.getActiveNetworkInfo();
                                   if (networkInfo != null && networkInfo.isConnected()) {
                                       Geocoder gc = new Geocoder(context, Locale.getDefault());
                                       String text;
                                       try {
                                           List<Address> addresses = gc.getFromLocation(latitute, longitude, 1);
                                           String address = addresses.get(0).getAddressLine(0);
                                           String city = addresses.get(0).getLocality();
                                           String state = addresses.get(0).getAdminArea();
                                           String country = addresses.get(0).getCountryName();
                                           String postalCode = addresses.get(0).getPostalCode();
                                           String knownName = addresses.get(0).getFeatureName();
                                           text = address + "\n" + city + "\n" + state + "\n" + country + "\n" + postalCode + "\n" + knownName;
                                           sendMessage(number, text);
                                       } catch (Exception e1) {
                                           text = "Error";
                                           sendMessage(number, text);
                                       }
                                       mydb.insertIntoMessages(number, body, currentDateAndTime);
                                       mydb.insertIntoMessages("reply", text, currentDateAndTime);
                                   } else {
                                       String text = "Latitude :" + latitute + "\n" + "Longitude :" + longitude;
                                       sendMessage(number, text);
                                       mydb.insertIntoMessages(number, body, currentDateAndTime);
                                       mydb.insertIntoMessages("reply", text, currentDateAndTime);
                                   }
                               }
                           } else if (arr[2].equalsIgnoreCase("ChangeProfile") && ringProfile_switchOn) {

                               try {
                                   int l = arr[3].length();
                               } catch (Exception e) {

                                   mydb.insertIntoMessages(number, body, currentDateAndTime);

                                   AudioManager am;
                                   am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                                   am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                                   sendMessage(number, "Success");
                                   mydb.insertIntoMessages("reply", "Done", currentDateAndTime);
                               }
                           } else if (arr[2].equalsIgnoreCase("SetScreenLock") && lockMobileScreen_switchOn) {
                               try {
                                   int l = arr[3].length();
                               } catch (Exception e) {
                                   DevicePolicyManager devicePolicyManager = (DevicePolicyManager) context.getSystemService(context.DEVICE_POLICY_SERVICE);
                                   ComponentName demoDeviceAdmin = new ComponentName(context, MyAdmin.class);
                                   boolean admin = devicePolicyManager.isAdminActive(demoDeviceAdmin);
                                   if (admin) {
                                       devicePolicyManager.lockNow();
                                   }
                                   String text = "Success";
                                   sendMessage(number, text);
                                   mydb.insertIntoMessages(number, body, currentDateAndTime);
                                   mydb.insertIntoMessages("reply", text, currentDateAndTime);
                               }
                           }
                       }else if (msgLength == 2 && arr[0].equalsIgnoreCase("mobifix")) {
                           if (arr[1].equalsIgnoreCase("done")) {
                               //  this.finish();
//                            getActivity().stopService(new Intent(getActivity(), SmsReceiver.class));
                               try {

                                   FirebaseAuth.getInstance().signOut();
                                   SharedPreferences prefs = context.getSharedPreferences("isFirst", Context.MODE_PRIVATE);
                                   SharedPreferences.Editor myEdit = prefs.edit();

                                   myEdit.putBoolean("LoginActivity", true);
                                   myEdit.commit();
                                   String s = "Success";
                                   Toast.makeText(context, "signed out", Toast.LENGTH_SHORT).show();
                                   sendMessage(number, s);
                               } catch (Exception e) {
                                   String s = "Signout unsuccessful";
                                   sendMessage(number, s);
                                   Toast.makeText(context, "not signed out", Toast.LENGTH_SHORT).show();
                               }
                           }
                       }
                       }
                   }
               }

        }else {

            Log.v("ajay", "Something received else");

        }
    }

    private void finish() {
        System.runFinalizersOnExit(true);
        System.exit(0);
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    public String getContact(String ContactName, Context context) {

        ContactName=ContactName.substring(0,ContactName.length()-1);

        String[] projection=new String[]{
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER,
        };

        Cursor cursor=null;
        try {
            cursor=context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,projection,null,null,null);
        } catch (SecurityException e1){
            //
        }

        if(cursor!=null){
            try{
                HashSet<String> normalizedNumbersAlreadyFound=new HashSet<>();
                int indexOfNormalizedNumber=cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER);
                int indexOfDisplayName=cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                int indexOfDisplayNumber=cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

                while (cursor.moveToNext()){
                    String normalizedNumber=cursor.getString(indexOfNormalizedNumber);
                    if(normalizedNumbersAlreadyFound.add(normalizedNumber)){
                        String displayName=cursor.getString(indexOfDisplayName);
                        String displayNumber=cursor.getString(indexOfDisplayNumber);

                        Log.i("number",displayNumber);

                        if(ContactName.equalsIgnoreCase(displayName)){
                            cursor.close();
                            return displayNumber;
                        }
                    }
                }
            } finally {
                cursor.close();
            }
        }
        return "Unsaved";
    }
    public void sendMultiMessage(String number,String text) {
        SmsManager smsManager = SmsManager.getDefault();
        ArrayList<String>parts=smsManager.divideMessage(text);
        smsManager.sendMultipartTextMessage(number, null, parts, null, null);
    }

    public void sendMessage(String number,String text) {
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(number, null, text, null, null);

    }
}
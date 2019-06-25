package com.nfc.ta;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;
import android.widget.Toast;

public class AbsenDosenActivity extends Activity {

	static final int DIALOG_ERROR_CONNECTION = 1;
	public String jsonResult;
	String serverurl;
	
	TextView NrpMhs, NamaMK, Kls, NamaDosen, TglAbsen;
	String nrp, tgl, status;
	String mk, kelas_mk, nama_dosen;
	String nrp_nfc, mk_nfc, kls_nfc, dsn_nfc, tgl_nfc, status_nfc;
	
	/**
	 * Create AbsenActivity
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_absen_dosen);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		/**
		 * Sound Effects Initialization
		 */
		SoundEffects.getInstance().init(this);
	}

	/**
     * Checking Connectivity
     */
	public boolean isOnline(Context c) {
    	ConnectivityManager cm = (ConnectivityManager) c
    			.getSystemService(Context.CONNECTIVITY_SERVICE);
    	NetworkInfo ni = cm.getActiveNetworkInfo();
    	
    	if (ni != null && ni.isConnected()) {
    		return true;
    	}
    	else {
    		return false;
    	}
    }
	
	/**
     * Create Dialog Message
     */
	public Dialog onCreateDialog(int id) {
    	Dialog dialog = null;
    	switch (id) {
    	case DIALOG_ERROR_CONNECTION:
    		AlertDialog.Builder errorDialog = new AlertDialog.Builder(this);
    		errorDialog.setIcon(android.R.drawable.ic_dialog_alert);
    		errorDialog.setTitle("Error");
    		errorDialog.setMessage("No Internet Connection !");
    		
    		errorDialog.setNeutralButton("OK",
    				new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int id) {
							dialog.dismiss();
							finish();
							new Handler().postDelayed(new Runnable() {
								
								@Override
								public void run() {
									startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS), 0);
									return;
								}
							}, 1000);
						}
					});
    		
    		AlertDialog errorAlert = errorDialog.create();
    		return errorAlert;
    		
    		default:
    			break;
    	}
    	return dialog;
    }
	
	/**
	 *  Check to see that the Activity started due to an Android Beam
	 */
	public void onResume() {
		super.onResume();
		if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
			processIntent(getIntent());
		}
	}
	
	/**
	 * onResume gets called after this to handle the intent
	 */
	public void onNewIntent(Intent intent) {
		setIntent(intent);
	}
	
	/**
	 * Parses the NDEF Message from the intent and sends to the Server via JSON
	 */
	void processIntent(Intent intent) {
		SharedPreferences Pref  = this.getSharedPreferences( "ServerData", Context.MODE_PRIVATE);
        serverurl = Pref.getString("server",null);
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(
                NfcAdapter.EXTRA_NDEF_MESSAGES);
	    // only one message sent during the beam
	    NdefMessage msg = (NdefMessage) rawMsgs[0];
	    
	    // record 0 contains the MIME type  
	    String payload = new String(msg.getRecords()[0].getPayload());
	    
	    /**
	     * Split payload value
	     */
	    String[] separated = payload.split(":");
	    nrp = separated[0];
	    mk = separated[1];
	    kelas_mk = separated[2];
	    nama_dosen = separated[3];
	    status = separated[4];
	    
	    NrpMhs = (TextView)this.findViewById(R.id.nrp_nfc);
		NamaMK = (TextView)this.findViewById(R.id.nama_mk);
		Kls = (TextView)this.findViewById(R.id.kls_mk);
		NamaDosen = (TextView)this.findViewById(R.id.nama_dosen);
	    
		NrpMhs.setText(nrp);
		NamaMK.setText(mk);
		Kls.setText(kelas_mk);
		NamaDosen.setText(nama_dosen);
		
	    if (!isOnline(AbsenDosenActivity.this)) {
    		showDialog(DIALOG_ERROR_CONNECTION);
    	}
	    
	    else {
    			AbsenTask task = new AbsenTask();
    			try {
    				nrp_nfc = URLEncoder.encode(nrp.toString(),"UTF-8");
    				mk_nfc = URLEncoder.encode(mk.toString(),"UTF-8");
    				kls_nfc = URLEncoder.encode(kelas_mk.toString(),"UTF-8");
    				dsn_nfc = URLEncoder.encode(nama_dosen.toString(),"UTF-8");
    				status_nfc = URLEncoder.encode(status.toString(),"UTF-8");
    				String url = "http://"+serverurl+"/Login.php?function=MasukManual&nrp=" + nrp_nfc + "&mk=" + mk_nfc + "&kls=" + kls_nfc + "&nip=" + dsn_nfc + "&status=" + status_nfc +"";
    				task.execute(url);
    			} catch (UnsupportedEncodingException e) {
    				e.printStackTrace();
    			}
    	}
	    
	}
	
	/**
     * Class for execute Http URL
     */
	private class AbsenTask extends AsyncTask<String, Integer, String> {
    	ProgressDialog prDialog;

    	@Override
    	protected void onPreExecute() {
    		prDialog=ProgressDialog.show(AbsenDosenActivity.this, "", "Loading . . .");
    	};
    	
		@Override
		protected String doInBackground(String... urls) {
			StringBuilder builder = new StringBuilder();
			HttpClient client = new DefaultHttpClient();
			HttpGet httpGet = new HttpGet(urls[0]);
			Log.i("urls[0]",urls[0]);
			try {
				HttpResponse response = client.execute(httpGet);
				StatusLine statusLine = response.getStatusLine();
				int statusCode = statusLine.getStatusCode();
				if(statusCode == 200) {
					HttpEntity entity = response.getEntity();
					InputStream content = entity.getContent();
					BufferedReader reader = new BufferedReader(new InputStreamReader(content));
					String line;
					while ((line = reader.readLine()) != null) {
						builder.append(line);
					}
				}
				else {
					Log.e("Failed", "Failed to download file");
				}
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return builder.toString();
		}
		
		@Override
		protected void onProgressUpdate(Integer... progress) {
			setProgress(progress[0]);
		}
		
		protected void onPostExecute(String result) {
			if (!isOnline(AbsenDosenActivity.this)) {
				showDialog(DIALOG_ERROR_CONNECTION);
			}
			else {
				prDialog.dismiss();
				jsonResult = result;
				Toast.makeText(getApplicationContext(), jsonResult, Toast.LENGTH_LONG).show();
				SoundEffects.getInstance().playSound(SoundEffects.SOUND_1);
				finish();
			}
		}
    }

}

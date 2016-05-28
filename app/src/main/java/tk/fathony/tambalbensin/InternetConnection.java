package tk.fathony.tambalbensin;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by jeki on 4/27/16.
 */
public abstract class InternetConnection implements Handler.Callback{
    public final String GET="GET";
    private final Context context;
    private final long DELAY=3000;
    private final boolean persistent;
    private int attempt=0;
    private final int MAX_ATTEMPT=3;
    private Bundle data = new Bundle();
    private Thread thread;
    private HttpURLConnection httpConn;

    public InternetConnection(Context ctx){
        this(ctx,false);
    }

    public InternetConnection(Context ctx, boolean persistent){
        this.context = ctx;
        this.persistent = persistent;
    }

    public void get(String url){
        request(url, null, GET);
    }

    public void get(String url,Bundle params){
        url +="?";
        for(String key : params.keySet()) {
            String value = params.getString(key);
            url += key+"="+value+"&";
        }
        Log.i("jeki", url);
        attempt=0;
        request(url, null, GET);
    }


    private void request(final String url,final Bundle params,final String method){

        if(thread!=null) thread.interrupt();
        if(httpConn!=null)httpConn.disconnect();

        data.putString("url", url);
        data.putBundle("params", params);
        data.putString("method", method);
        thread = new Thread() {
            public void run() {
                Message msg = Message.obtain();
                msg.what = 1;
                Bundle b;
                String response = null;
                try {
                    response = openHttpConnection(url, params, method);
                    b = new Bundle();
                    b.putString("response", response);
                    b.putString("url", url);
                    b.putInt("response_code", 200);
                } catch (IOException e) {
                    e.printStackTrace();
                    b = new Bundle();
                    b.putInt("response_code", 408);
                } catch (NullPointerException e){
                    e.printStackTrace();
                    b = new Bundle();
                    b.putInt("response_code", 404);
                }

                msg.setData(b);
                handleMessage(msg);
            }
        };
        thread.start();
    }


    @Override
    public boolean handleMessage(Message msg) {
        try{
            int code = msg.getData().getInt("response_code");

            if(code==200){
                String resp =msg.getData().getString("response");

                OnSuccess(resp);
            }else{
                OnFailed();
            }
        }catch(JSONException e){
            e.printStackTrace();
            OnFailed();
        }catch(NullPointerException nu){
            OnNull(nu);
        }
        return false;
    }

    abstract protected void OnSuccess(String result) throws JSONException;

    protected void TryAgain(){
        String url = data.getString("url");
        Bundle params = data.getBundle("params");
        String method = data.getString("method");
        request(url, params, method);
    }

    protected void OnFailed(){
        attempt++;
        Log.i("jeki","attempt "+attempt);
        if(attempt>=MAX_ATTEMPT && !persistent){
            OnTimeout();
            return;
        }
        try{
            Thread.sleep(DELAY);
            TryAgain();
        }catch (InterruptedException ex){ Log.i("jeki","another process going on");}
    }

    public void resetAndTryAgain(){
        attempt=0;
        TryAgain();
    }

    protected void OnTimeout(){

    }

    protected void OnNull(NullPointerException ex){
        ex.printStackTrace();
    }


    public String openHttpConnection(String urlStr,Bundle params,String method)
            throws IOException, NullPointerException {

        int resCode = -1;
        String total = null;
        if(!isConnected()){
            throw new NullPointerException("Not Connected");
        }

        try {

            URL url = new URL(urlStr);
            httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setAllowUserInteraction(false);
            httpConn.setInstanceFollowRedirects(false);
            httpConn.setRequestMethod(method);
            httpConn.setConnectTimeout(15000);

            String param = "";
            if(params!=null) {
                OutputStream os = httpConn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                param = encodeParams(params);
                writer.write(param);
                writer.flush();
                writer.close();
            }

            httpConn.connect();

            resCode = httpConn.getResponseCode();
            if (resCode == HttpURLConnection.HTTP_OK) {

                total = getReponse(httpConn);

            }else if(resCode==HttpURLConnection.HTTP_INTERNAL_ERROR){
                URL errorurl = new URL(urlStr+"?"+param);
                throw new NullPointerException(" response code "+resCode+"method"+method+" "+errorurl);
            }else{
                URL errorurl = new URL(urlStr+"?"+param);
                throw new NullPointerException(" response code "+resCode+"method"+method+" "+errorurl);
            }

            if(total==null) throw new NullPointerException("respon kosong "+urlStr+param);
        }catch (MalformedURLException e) {
            e.printStackTrace();
        }
        finally {
            if(httpConn!=null){
                httpConn.disconnect();
            }
        }

        return total;
    }

    private String getReponse(HttpURLConnection http) throws IOException{
        InputStream in = http.getInputStream();
        StringBuilder total = new StringBuilder();
        BufferedReader r = new BufferedReader(new InputStreamReader(in));

        String line;
        while ((line = r.readLine()) != null) {
            total.append(line);
        }
        return total.toString();
    }

    private String encodeParams(Bundle params){
        String  strParam = "";
        for (String key : params.keySet()){
            strParam += key + "=" + params.getString(key) + "&";
        }
        strParam = strParam.substring(0,strParam.length()-1);
        return strParam;
    }

    public boolean isConnected() {
        return isConnected(context);
    }

    public static boolean isConnected(Context ctx) {
        ConnectivityManager connec =(ConnectivityManager) ctx.getSystemService(ctx.CONNECTIVITY_SERVICE);

        if ( connec.getNetworkInfo(0).getState() == android.net.NetworkInfo.State.CONNECTED ||

                connec.getNetworkInfo(0).getState() == android.net.NetworkInfo.State.CONNECTING ||
                connec.getNetworkInfo(1).getState() == android.net.NetworkInfo.State.CONNECTING ||
                connec.getNetworkInfo(1).getState() == android.net.NetworkInfo.State.CONNECTED ) {

            return true;
        }else if (
                connec.getNetworkInfo(0).getState() == android.net.NetworkInfo.State.DISCONNECTED ||
                        connec.getNetworkInfo(1).getState() == android.net.NetworkInfo.State.DISCONNECTED  ) {
            return false;
        }
        return false;
    }


}

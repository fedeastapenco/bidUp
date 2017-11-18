package proyectointegrador.bidup.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.cloudinary.Cloudinary;
import com.cloudinary.android.MediaManager;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import proyectointegrador.bidup.Manifest;
import proyectointegrador.bidup.R;
import proyectointegrador.bidup.helpers.HttpConnectionHelper;
import proyectointegrador.bidup.helpers.HttpRequestMethod;
import proyectointegrador.bidup.models.Auction;
import proyectointegrador.bidup.models.User;

public class CreateAuctionActivity extends AppCompatActivity {
    private CreateAuctionTask mCreateAuctionTask = null;
    public static final String PREFS_NAME = "MyPrefsFile";
    private EditText mObjectName;
    private EditText mInitialAmount;
    static final int TAKE_PHOTO = 100;
    String mCurrentPhotoPath;
    String urlPhoto = null;
    //TODO agregar el input para date
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_create_auction);
            mObjectName = (EditText)findViewById(R.id.create_auction_objectName);
            mInitialAmount = (EditText)findViewById(R.id.create_auction_initial_amount);
            Button mCreateAuction = (Button) findViewById(R.id.btn_create_auction);
            mCreateAuction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    attemptCreateAuction();
                }
            });


        }catch (Exception ex){
            Log.i("ERROR: " , ex.getMessage());
        }

    }
    public void takePhoto(View view){
       try {
           if (Build.VERSION.SDK_INT >= 23 &&
                   ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA ) != PackageManager.PERMISSION_GRANTED) {
               ActivityCompat.requestPermissions(this,new String[]{android.Manifest.permission.CAMERA},0);
           }else{
               Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
               if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                   File photoFile = null;
                   try {
                       photoFile = createImageFile();
                   } catch (IOException ex) {
                       // Error occurred while creating the File
                   }
                   // Continue only if the File was successfully created
                   if (photoFile != null) {
                       Uri photoURI = FileProvider.getUriForFile(this,
                               "proyectointegrador.bidup.fileprovider",
                               photoFile);
                       takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                       startActivityForResult(takePictureIntent, TAKE_PHOTO);
                   }
               }
           }
       }catch (Exception ex)
       {
           Log.i("ERROR: " , ex.getMessage());

       }

    }


    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }
//    private Uri getOutputMediaFile() {
//        Uri uri = null;
//        try {
//            File mediaStoreDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"bidUpFotos");
//            if(!mediaStoreDir.exists()){
//                mediaStoreDir.mkdir();
//            }
//            String timeStamp = new SimpleDateFormat("yyyyMMdd_hhmmss").format(new Date());
//            File mediaFile = new File(mediaStoreDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
//            uri = Uri.fromFile(mediaFile);
//        }catch(Exception ex){
//            Log.e("Error al crear archivo", ex.getMessage());
//        }
//        return uri;
//    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            if(requestCode == TAKE_PHOTO){
                if(resultCode == RESULT_OK){
                    Toast.makeText(this, "Imagen guardada en : \n" + mCurrentPhotoPath + ", la imagen se está subiendo a internet", Toast.LENGTH_LONG).show();
                    //urlPhoto = MediaManager.get().url().generate(mCurrentPhotoPath);
                     //MediaManager.get().getCloudinary().uploader().unsignedUpload(photo,"s1sqbyy4",new HashMap(0));
                    //urlPhoto = MediaManager.get().url().generate(mCurrentPhotoPath);
                }
            }
        }catch (Exception ex){
            Log.e("Error al volver de foto", ex.getMessage());
        }
    }

    private void attemptCreateAuction(){
        if (mCreateAuctionTask != null) {
            return;
        }
        mObjectName.setError(null);
        mInitialAmount.setError(null);
        String objectName = mObjectName.getText().toString();
        String initialAmount = mInitialAmount.getText().toString();

        boolean cancel = false;
        View focusView = null;
        if(TextUtils.isEmpty(objectName)){
           mObjectName.setError("Campo requerido");
           focusView = mObjectName;
           cancel = true;
        }
        if(TextUtils.isEmpty(initialAmount)){
            mInitialAmount.setError("Campo requerido");
            focusView = mInitialAmount;
            cancel = true;
        }
        if(cancel){
            //hay error
            focusView.requestFocus();
        }else{
            Auction auction = new Auction();
            auction.setObjectName(objectName);
            auction.setInitialAmount(Double.parseDouble(initialAmount));
            mCreateAuctionTask = new CreateAuctionTask(this,auction);
            mCreateAuctionTask.execute("/auction/create/");
        }

    }
    //TODO subo la imagen aca o cuando la saca?
    private class CreateAuctionTask extends AsyncTask<String, Void, Auction>{
        private Activity activity;
        private Auction auction;
        CreateAuctionTask(Activity activity, Auction auction){
            this.activity = activity;
            this.auction = auction;
        }
        @Override
        protected Auction doInBackground(String... params) {
            try {
                
                HttpURLConnection urlConnection = HttpConnectionHelper.CreateConnection(HttpRequestMethod.POST, params);
                JSONObject objectToSend = new JSONObject();
                objectToSend.put("objectName", auction.getObjectName());
                objectToSend.put("initialAmount", auction.getInitialAmount());
                objectToSend.put("lastDate", auction.getLastDate());
                JSONObject response = HttpConnectionHelper.SendRequest(urlConnection, objectToSend, getSharedPreferences(PREFS_NAME,0));
                if(response == null){
                    //no se pudo crear el usuario
                    return null;
                }else{
                    Auction ret = new Auction();
                    //TODO created y LastDate
                    //ret.setCreated(response.getString("created"));
                    //ret.setLastDate();
                    ret.setInitialAmount(response.getDouble("initialAmount"));
                    ret.setObjectName(response.getString("objectName"));

                    return ret;
                }
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(Auction auction) {
            TextView tv = (TextView) findViewById(R.id.txt_error_create_auction);
            if(auction == null){
                tv.setText("No se pudo crear la subasta");
                tv.setVisibility(View.VISIBLE);
            }else {
                Intent intent = new Intent(CreateAuctionActivity.this, MainActivity.class);
                startActivity(intent);
            }
        }
    }
}

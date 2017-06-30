package szalai.hu.camerapermission;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST = 1;
    private static final int CAMERA_REQUEST_CODE = 8675309;
    private static final int WRITE_EXTERNAL_REQUEST_CODE = 2;
    private static final int INTERNET_REQUEST_CODE = 3;
    private ImageView imageView;
    private Bitmap bitmap;
    private String encodedString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button = (Button) findViewById(R.id.button1);
        imageView = (ImageView) findViewById(R.id.imageView);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        takePhoto();
                    } else {
                        String[] permissonrequest = {Manifest.permission.CAMERA};
                        requestPermissions(permissonrequest, CAMERA_REQUEST_CODE);
                    }
                } else {
                    takePhoto();
                }
            }
        });
        Button button2 = (Button) findViewById(R.id.button2);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        if (bitmap != null) {
                            saveImage(bitmap);
                        }
                    } else {
                        String[] permissonrequest = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
                        requestPermissions(permissonrequest, WRITE_EXTERNAL_REQUEST_CODE);
                    }
                } else {
                    if (bitmap != null) {
                        saveImage(bitmap);
                    }
                }
            }
        });
        Button button3 = (Button) findViewById(R.id.button3);
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED) {
                        if (bitmap != null) {
                            uploadPhoto();
                        }
                    } else {
                        String[] permissonrequest = {Manifest.permission.INTERNET};
                        requestPermissions(permissonrequest, INTERNET_REQUEST_CODE);
                    }
                } else {
                    if (bitmap != null) {
                        uploadPhoto();
                    }
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePhoto();
            } else {
                Toast.makeText(MainActivity.this, "You cant take a photo without permisson", Toast.LENGTH_LONG).show();
            }
        }
        if (requestCode == WRITE_EXTERNAL_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (bitmap != null) {
                    saveImage(bitmap);
                }
            } else {
                Toast.makeText(MainActivity.this, "You cant save a photo without permisson", Toast.LENGTH_LONG).show();
            }
        }
        if (requestCode == INTERNET_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (bitmap != null) {
                    uploadPhoto();
                }
            } else {
                Toast.makeText(MainActivity.this, "You cant upload a photo without permisson", Toast.LENGTH_LONG).show();
            }
        }

    }

    private void takePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, CAMERA_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i("info", "requestcode " + requestCode + " resultcode " + resultCode);
        if (requestCode == CAMERA_REQUEST) {
            super.onActivityResult(requestCode, resultCode, data);
            bitmap = (Bitmap) data.getExtras().get("data");
            imageView.setImageBitmap(bitmap);
        }
    }

    private void saveImage(Bitmap finalBitmap) {

        String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
        Log.i("info", root);
        Random generator = new Random();
        int n = 10000;
        n = generator.nextInt(n);
        String fname = "Image-" + n + ".jpg";
        File file = new File(root, fname);
        if (file.exists()) file.delete();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void uploadPhoto() {
        new EncodeImage().execute();


    }


    private class EncodeImage extends AsyncTask<Void, Void, Void> {



        @Override
        protected Void doInBackground(Void... params) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            encodedString = Base64.encodeToString(byteArray, Base64.DEFAULT);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            makeRequest();
        }
    }

    private void makeRequest() {
        Log.i("info", "makeRequest()");
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        StringRequest request = new StringRequest(Request.Method.POST, "http://szemelyek.szalaimihaly.hu/upload.php",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.i("info", response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.i("info", "error: " + error);
                    }

                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String imageName = "JPEG_" + timeStamp + ".jpg";
                HashMap<String, String> map = new HashMap<>();
                map.put("image", encodedString);
                Log.i("info", encodedString);
                //map.put("image", imageName);
                return map;
            }
        };
        requestQueue.add(request);
    }
}

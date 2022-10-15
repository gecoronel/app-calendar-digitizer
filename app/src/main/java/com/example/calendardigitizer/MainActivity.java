package com.example.calendardigitizer;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.CalendarContract;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;


public class MainActivity extends AppCompatActivity {

    private EditText et_evento_lila;
    private EditText et_evento_amarillo;
    private EditText et_evento_celeste;
    private Button btn_elegir_cal;

    private static final int REQ_CODE_CAMARA = 0;
    private static final int REQ_CODE_GALERIA = 1;

    private static final int WRITE_EXTERNAL_STORAGE = 101;
    private static final int READ_EXTERNAL_STORAGE = 102;
    private static final int INTERNET = 103;
    private static final int WRITE_CALENDAR = 104;
    private static final int READ_CALENDAR = 105;

    private Context ctx = this;

    private String realPath;
    private String etiqueta1;
    private String etiqueta2;
    private String etiqueta3;

    private ProgressDialog pd;


    String rta = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Comprobar los 3 permisos necesarios para el funcionamiento de la app
        checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE);
        checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE);
        checkPermission(Manifest.permission.INTERNET, INTERNET);
        checkPermission(Manifest.permission.WRITE_CALENDAR, WRITE_CALENDAR);
        checkPermission(Manifest.permission.READ_CALENDAR, READ_CALENDAR);

        // Cargar calendarios de ejemplo a la galería del teléfono
        try {
            cargarImagenesEjemplo();
        } catch (IOException e) {
            e.printStackTrace();
        }

        et_evento_lila = (EditText) findViewById(R.id.et_lila);
        et_evento_amarillo = (EditText) findViewById(R.id.et_amarillo);
        et_evento_celeste = (EditText) findViewById(R.id.et_celeste);

        et_evento_lila.addTextChangedListener(textWatcher);
        et_evento_amarillo.addTextChangedListener(textWatcher);
        et_evento_celeste.addTextChangedListener(textWatcher);

        btn_elegir_cal = (Button) findViewById(R.id.btn_elegir_calend);
        btn_elegir_cal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImage(ctx);
                etiqueta1 = et_evento_lila.getText().toString();
                etiqueta2 = et_evento_amarillo.getText().toString();
                etiqueta3 = et_evento_celeste.getText().toString();
            }
        });

        pd = new ProgressDialog(this);

    }

    // función que corre al iniciar la app para cargar imágenes de prueba. Se guardaran en
    // el almacenamiento interno en el directorio Digicalendar
    private void cargarImagenesEjemplo() throws IOException {

        File filepath = Environment.getExternalStorageDirectory();
        String pathImages = filepath.getAbsolutePath()+"/CalendarDigitizer/";
        File dir = new File(pathImages);

        if (! dir.exists()){
            dir.mkdir();
        }else{
            // ya se descargaron las imágenes pruebas
            return;
        }

        for (int i = 1; i <= 20; i++) { // cargar del calendario 1 al 5 inclusivo nomás.. los otros los puse para probar

            // nombre de la imagen
            String nombre = "calendario" + i + ".jpg";

            // cargar imagen demo
            InputStream ims = getAssets().open(nombre);
            Bitmap bitmap = BitmapFactory.decodeStream(ims);

            // crear directorio en donde guardar
            File file = new File(dir, nombre);
            OutputStream os = new FileOutputStream(file);

            // guardar imagen
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);

            // para que aparezca las imágenes en la galería
            MediaScannerConnection.scanFile(ctx,
                    new String[] {filepath.getAbsolutePath()+"/CalendarDigitizer/"+nombre}, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        @Override
                        public void onScanCompleted(String s, Uri uri) {
                            Log.i("TAG", "Finished scanning");
                        }
                    });

        }

    }

    // Función para habilitar/deshabilitar el botón de elegir calendario
    // según si se completaron los campos de los nombres de los eventos
    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            String ev1 = et_evento_lila.getText().toString().trim();
            String ev2 = et_evento_amarillo.getText().toString().trim();
            String ev3 = et_evento_celeste.getText().toString().trim();
            btn_elegir_cal.setEnabled(!ev1.isEmpty() && !ev2.isEmpty() && !ev3.isEmpty());
        }

        @Override
        public void afterTextChanged(Editable editable) {
        }
    };

    // Función lanzada luego de presionar el botón elegir calendario. Muestra en pantalla la
    // posibilidad de tomar una foto o elegir desde la galería
    private void selectImage(Context ctx) {

        final CharSequence[] options = {"Tomar Foto", "Abrir Galería", "Cancelar"};

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle("Elige el Calendario");

        builder.setItems(options, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int item) {

                if (options[item].equals("Tomar Foto")) {
                    Intent takePicture = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(takePicture, REQ_CODE_CAMARA);

                } else if (options[item].equals("Abrir Galería")) {
                    Intent pickPhoto = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(pickPhoto, REQ_CODE_GALERIA);

                } else if (options[item].equals("Cancelar")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    //@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_CANCELED) {
            switch (requestCode) {
                case REQ_CODE_CAMARA:
                    if (resultCode == RESULT_OK && data != null) {
                        Bitmap selectedImage = (Bitmap) data.getExtras().get("data");

                        // obtener uri a partir del Bitmap y luego la dirección en donde se encuentra la imagen dentro del almacenamiento
                        Uri tempUri = getImageUri(getApplicationContext(), selectedImage);
                        realPath = getPath(tempUri);
                        // iniciar actividad asíncrona para enviar imagen por http, mostrar pantalla de que se está procesando, ....
                        uploadImage(realPath);
//                        SendImage start_task = new SendImage();
//                        start_task.execute();

                    }
                    break;

                case REQ_CODE_GALERIA:
                    if (resultCode == RESULT_OK && data != null) {
                        Uri selectedImage = data.getData();

                        if (selectedImage != null) {

                            // obtener la dirección en donde se encuentra la imagen dentro del almacenamiento
                            realPath = getPath(selectedImage);
                            // iniciar actividad asíncrona para enviar imagen por http, mostrar pantalla de que se está procesando, ....
                            uploadImage(realPath);
//                            SendImage start_task = new SendImage();
//                            start_task.execute();

                        }
                    }
                    break;
            }
        }
    }

    private Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path.toString());
    }

    // Obtiene la dirección en donde está guardada la imagen dentro del almacenamiento
    private String getPath(Uri uri) {
        String result;
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor == null) { // Source is Dropbox or other similar local file path
            result = uri.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    // Envía por http la imagen y obtiene la respuesta en la función: public void onResponse(...)
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void uploadImage(String realPath) {
        File file = new File(realPath);
        Retrofit retrofit = NetworkClient.getRetrofit();

        RequestBody requestBody = RequestBody.create(MediaType.parse("*/*"), file);
        MultipartBody.Part parts = MultipartBody.Part.createFormData("upload", file.getName(), requestBody);

        RequestBody filename = RequestBody.create(MediaType.parse("text/plain"), file.getName());

        UploadApis uploadApis = retrofit.create(UploadApis.class);
        Call call = uploadApis.uploadImage(parts, filename);

        final ProcesandoDialog dialog = new ProcesandoDialog(this);
        dialog.setMensaje("Procesando imagen...");
//        dialog.cambiarImagenProgressBar(ctx.getDrawable(R.drawable.processing));

        call.enqueue(new Callback() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @SuppressLint("NewApi")
            @Override
            public void onResponse(Call call, Response response) {

                if(!response.isSuccessful()){

                    dialog.cambiarImagenProgressBar(ctx.getDrawable(R.drawable.cross));
                    dialog.setMensaje("Problema en la comunicación con el servidor");
                    dialog.stopDialog(5);  // cerrar la ventana luego de 3 segundos

                }else {

                    try { rta = ((ResponseBody) response.body()).string();
                    } catch (IOException e) { e.printStackTrace(); }

                    rta = rta.replace("\"", "");

                    // si la respuesta es un error, vendrá como: error x, donde x será un número identificativo
                    if (rta.toLowerCase().contains("error")) {

                        rta = rta.replace("error ", "");
                        String mensaje = obtenerMensajeError(rta);

                        dialog.cambiarImagenProgressBar(ctx.getDrawable(R.drawable.cross));
                        dialog.setMensaje(mensaje);
                        dialog.stopDialog(5);  // cerrar la ventana luego de 3 segundos

                    } else { // respuesta correcta
                        new EventCreation().execute();
                        dialog.stopDialog(0);  // cerrar la ventana luego de 3 segundos
                    }

                }

            }

            private String obtenerMensajeError(String n_error) { // función anonima interna
                switch (n_error) {
                    case "1": return "Imagen desenfocada. \n Intente nuevamente";
                    case "2": return "La imagen no supera el brillo estipulado. \n Intente nuevamente";
//                    case "3": return "No se pudieron detectar los meses";
//                    case "4": return "No se pudo detectar en qué dia comienza la semana";
                    default: return "No se pudo detectar eventos.\n Intente nuevamente";
                }
            }

            @Override
            public void onFailure(Call call, Throwable t) {
                Toast.makeText(getApplicationContext(), t.getMessage(), Toast.LENGTH_LONG).show();

                dialog.cambiarImagenProgressBar(ctx.getDrawable(R.drawable.cross));
                dialog.setMensaje("Problema en la comunicación con el servidor");
                dialog.stopDialog(5);  // cerrar la ventana luego de 3 segundos

            }
        });
    }


    public List<Evento> crearEventos(String str) {
        List<Evento> eventos = new ArrayList<Evento>();

        Scanner cadena_scan = new Scanner(str);
        cadena_scan.useDelimiter(";");

        List<String> eventos_str = new ArrayList<String>();
        while (cadena_scan.hasNext()) {
            eventos_str.add(cadena_scan.next());
        }

        // iterar por cada uno de los eventos detectados
        for (String evento_str : eventos_str) {

            Evento evento = new Evento();

            Scanner ev_scan = new Scanner(evento_str);

            ev_scan.useDelimiter(",");

            int i = 0;
            while (ev_scan.hasNext()) {

                String temp = ev_scan.next();

                if (i == 0) { // parsear el numero del mes
                    int mes = Integer.parseInt(temp);
                    evento.setMes(mes);
                } else if (i == 1) { // parsear el color
                    evento.setColor(temp);
                } else { // parsear los numeros de los días
                    Scanner dias_scan = new Scanner(temp);

                    dias_scan.useDelimiter("-");

                    List<String> dias_str = new ArrayList<String>();
                    while (dias_scan.hasNext()) {
                        dias_str.add(dias_scan.next());
                    }

                    List<Integer> dias = new ArrayList<Integer>();
                    for (String dia_str : dias_str) {
                        int dia = Integer.parseInt(dia_str);
                        dias.add(dia);
                    }
                    evento.setDias(dias);

                }
                i += 1;
            }

            eventos.add(evento);

        }
        return eventos;
    }

    private long createEvent(int dia, int mes, String title) {

        long calID = 3;
        long startMillis = 0;
        long endMillis = 0;
        long eventID;
        Calendar beginTime = Calendar.getInstance();
        beginTime.set(2020, mes, dia, 7, 30);
        startMillis = beginTime.getTimeInMillis();
        Calendar endTime = Calendar.getInstance();
        endTime.set(2020, mes, dia, 19, 45);
        endMillis = endTime.getTimeInMillis();

        ContentResolver cr = getContentResolver();
        ContentValues values = new ContentValues();
        values.put(CalendarContract.Events.DTSTART, startMillis);
        values.put(CalendarContract.Events.DTEND, endMillis);
        values.put(CalendarContract.Events.TITLE, title);
        values.put(CalendarContract.Events.DESCRIPTION, "Event created for CalendarDigitizer");
        values.put(CalendarContract.Events.CALENDAR_ID, calID);
        values.put(CalendarContract.Events.EVENT_TIMEZONE, "Argentina");
        Uri uri = cr.insert(CalendarContract.Events.CONTENT_URI, values);
        eventID = Long.parseLong(uri.getLastPathSegment());
        return eventID;
    }

    private void createReminder(long id) {
        long eventID = id;
        ContentResolver cr = getContentResolver();
        ContentValues values = new ContentValues();
        values.put(CalendarContract.Reminders.MINUTES, 15);
        values.put(CalendarContract.Reminders.EVENT_ID, eventID);
        values.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT);
        Uri uri = cr.insert(CalendarContract.Reminders.CONTENT_URI, values);
    }
    /////////////////////////////////////////////////////////////////////////////////////////////

    private class EventCreation extends AsyncTask<String, Integer, String> {

        final ProcesandoDialog dialog = new ProcesandoDialog(MainActivity.this);

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            dialog.cambiarImagenProgressBar(ctx.getDrawable(R.drawable.processing));
            dialog.setMensaje("Creando eventos, aguarde por favor.");

        }

        @Override
        protected String doInBackground(String... strings) {

            List<Evento> eventos = crearEventos(rta);
            int i = 0;
            for(Evento ev : eventos){

                List<Integer>  dias = ev.getDias();
                int mes = ev.getMes();
                mes = mes - 1;
                String color = ev.getColor();
                String title = null;


                if ("fucsia".equals(color)){
                    title = etiqueta1;
                } else if ("celeste".equals(color)) {
                    title = etiqueta2;
                } else if ("amarillo".equals(color)){
                    title = etiqueta3;
                }
                System.out.println("--- Evento "+i);
                System.out.println("-mes "+ev.getMes());
                System.out.println("-titulo "+title);
                //System.out.println("-dias "+ev.getDias());
                i+=1;
                for(Integer dia : dias) {
                    long id = createEvent(dia, mes, title);
                    createReminder(id);
                    System.out.println("-dia "+dia);
                }
            }

            return "Terminamos la tarea en doInBackground" ;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            //pd.setProgress(values[0]);
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            dialog.stopDialog(1);  // cerrar la ventana luego de 3 segundos
            dialog.cambiarImagenProgressBar(ctx.getDrawable(R.drawable.tick));
            dialog.setMensaje("Los eventos han sido creados correctamente. Será dirigido al calendario.");
            dialog.stopDialog(10);  // cerrar la ventana luego de 3 segundos

            //Toast.makeText(MainActivity.this, "Eventos creados", Toast.LENGTH_SHORT).show();
            Log.e("Terminado: ", ""+s);

            et_evento_lila.setText("");
            et_evento_amarillo.setText("");
            et_evento_celeste.setText("");

            Uri.Builder builder = CalendarContract.CONTENT_URI.buildUpon();
            builder.appendPath("time");
            ContentUris.appendId(builder, Calendar.getInstance().getTimeInMillis());
            Intent intent = new Intent(Intent.ACTION_VIEW)
                    .setData(builder.build());
            startActivity(intent);

        }
    }



    /// ----------------------------------------------------------------------------
    /// Chekear los permisos y pedirselos al usuario ya que no basta con ponerlos en el manifest

    public void checkPermission(String permission, int requestCode)
    {
        if (ContextCompat.checkSelfPermission(MainActivity.this, permission)
                == PackageManager.PERMISSION_DENIED) {

            // Requesting the permission
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[] { permission },
                    requestCode);
        }

    }

    // ocultar teclado al tocar la pantalla
    public void ocultar_teclado(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }


}
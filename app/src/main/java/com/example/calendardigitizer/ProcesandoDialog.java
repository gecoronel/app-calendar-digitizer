package com.example.calendardigitizer;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class ProcesandoDialog {

    private Activity activity;
    private AlertDialog dialog;
    private TextView tv_mensaje;

    ProcesandoDialog(Activity myActivity ){
        activity = myActivity;

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        LayoutInflater inflater = activity.getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.procesando, null));
        builder.setCancelable(false);

        dialog = builder.create();
        dialog.show();

        tv_mensaje = dialog.findViewById(R.id.tv_mensaje);
    }

    void stopDialog(int seg){
        // cerrar dialog luego de x segundos
        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                dialog.dismiss();
                timer.cancel(); //this will cancel the timer of the system
            }
        }, seg * 1000);

    }

    public void setMensaje(String mensaje) {
        tv_mensaje.setText(mensaje);
        tv_mensaje.invalidate(); // for refreshment
    }

    public void cambiarImagenProgressBar(Drawable img) {
        ProgressBar pb = dialog.findViewById(R.id.progressBar);
        pb.setProgressDrawable(img);
        pb.setIndeterminateDrawable(img);
    }

}
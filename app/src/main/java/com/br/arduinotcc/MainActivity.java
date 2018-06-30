package com.br.arduinotcc;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private Spinner dispositivo;
    private Switch interruptor;
    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothSocket soquete = null;
    private OutputStream saida = null;
    private Set<BluetoothDevice> dispositivosPareados;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        interruptor = findViewById(R.id.interruptor);
        dispositivo = findViewById(R.id.dispositivo);


        ArrayAdapter<String> dados = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        dados.add("Selecione um dispositivo");

        if (bluetoothAdapter != null) {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                int REQUEST_ENABLE_BT = 1;
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }

            dispositivosPareados = bluetoothAdapter.getBondedDevices();
            if (dispositivosPareados.size() > 0) {
                for (BluetoothDevice item : dispositivosPareados) {
                    dados.add(item.getName());
                }
            }
        }
        dados.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dispositivo.setAdapter(dados);
    }

    private BluetoothSocket criarSoquete(BluetoothDevice device){
        Method method;
        BluetoothSocket tmpSoquete = null;

        try{
            method = device.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
            tmpSoquete = (BluetoothSocket) method.invoke(device,1);
        }catch (SecurityException e){

        }catch (NoSuchMethodException e){

        }catch (IllegalAccessException e){

        }catch (IllegalArgumentException e){

        }catch (InvocationTargetException e){

        }

        return tmpSoquete;
    }

    public void interruptorClicked(View v){
        String dados = "0";

        if(interruptor.isChecked()){
            dados = "1";
        }
        Toast mensagem = Toast.makeText(this.getApplicationContext(), " Enviando para "
                + dispositivo.getSelectedItem().toString() +
                (dados.equals("1")? " Ligar " : " Desligar "),
                Toast.LENGTH_LONG);

        mensagem.show();

        if(dispositivosPareados.size()>0){
            for(BluetoothDevice item : dispositivosPareados){
                if(dispositivo.getSelectedItem().toString()
                        .equalsIgnoreCase(item.getName())){
                    try {
                        BluetoothDevice dispRem = bluetoothAdapter.getRemoteDevice(item.getAddress());

                        Log.i("MAC", item.getAddress());
                        soquete = criarSoquete(dispRem);
                        soquete.connect();

                        bluetoothAdapter.cancelDiscovery();
                        saida = soquete.getOutputStream();
                        byte[] buffer = dados.getBytes();
                        saida.write(buffer);
                        saida.close();
                        Log.i("Saida",saida.toString());
                        soquete.close();
                    }catch (IOException e){

                    }
                }
            }
        }
    }
}


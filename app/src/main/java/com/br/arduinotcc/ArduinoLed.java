package com.br.arduinotcc;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

/*
A declaração da classe deve ser feita com a implementação do
View.OnCLickListener devido o layout possuir dois botões os quais
seus eventos devem ser capturados
 */
public class ArduinoLed extends AppCompatActivity implements View.OnClickListener{

    /*
    As variáveis decladas aqui são referentes aos
    componentes do layout
     */
    private Button ligar;
    private Button desligar;
    private Spinner dispositivos;

    /*
    Essas variáveis são referentes a:
    soquete - Conexão bluetooth
    bluetoothAdapter - o dispositivo bluetooth do celular
    pareados - quais são os dipositivos pareados com o smartphone
    arrayDispositivos - lista de dispositivos pareados
     */
    private BluetoothSocket soquete;
    private BluetoothAdapter bluetoothAdapter;
    private Set<BluetoothDevice> pareados;
    private ArrayAdapter<String>arrayDispositivos;

    /*
    Essa variável é referente a um componente
    que exibirá mensagens na tela de maneira dinâmica
     */
    private Toast msgTela;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_arduino_led);

        /*
        Métodos os quais serão explicados posteriormente
        mas a sequência deverá ser essa devido a utilização
         */
        inicializar();
        permissaoBluetooth();
        ligarBluetooth();
    }

    /*
    Esse método busca inicializar as variáveis anteriormente declaradas
    e também inicializar a captura de eventos dos botões
     */
    private void inicializar(){

        ligar = findViewById(R.id.ligar);
        desligar = findViewById(R.id.desligar);
        dispositivos = findViewById(R.id.listaPareados);
        arrayDispositivos = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item);

        ligar.setOnClickListener(this);
        desligar.setOnClickListener(this);
    }

    /*
    Esse método busca ligar o bluetooth do celular
    para sua utilização
     */
    private void ligarBluetooth(){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        //Verifica se o smartphone possui dispositivo bluetooth para ser utilizado
        if(bluetoothAdapter == null){
            msgTela = Toast.makeText(this.getApplicationContext()," O dispositivo não suporta Bluetooth ",Toast.LENGTH_LONG);
        }
        else{

            //Caso haja dispositivo ele pedirá permissão do usuário para ligar
            if(!bluetoothAdapter.isEnabled()){
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                int REQUEST_ENABLE_BT = 1;
                startActivityForResult(intent, REQUEST_ENABLE_BT);

            }
            //Verifica os dispositivos pareados
            dispositivosPareados(bluetoothAdapter);
        }
    }

    /*
    Esse método é para permissão em tempo de execução,
    como ele tá como exemplo apenas das permissões de
    segurança do Google, o preenchimento da resposta
    pode ser feito da maneira que desejar.
     */
    private void permissaoBluetooth(){
        int permissao = 1;
        int checarPermissao = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH);

        //Verifica se a palicação tem permissão para usar
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED){

        }
        //Caso não tenha permissão, ela será requisitada aqui
        else{
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH}, permissao);
        }
    }

    /*
    Esse método carrega todos os dispositivos pareados no celular
    para o componente Spinner do layout, será carregado apenas os
    nomes dos dispositivos e não seu MAC Address.
     */
    private void dispositivosPareados(BluetoothAdapter adapter){
        pareados = adapter.getBondedDevices();
        if (pareados.size() > 0){
            for(BluetoothDevice device : pareados){
                arrayDispositivos.add(device.getName());
            }
        }
        arrayDispositivos.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dispositivos.setAdapter(arrayDispositivos);
    }

    /*
    Esse método cria uma conexão direta entre o dispositivo selecionado
    e o smartphone a partir do método createRfcommSocket
     */
    private BluetoothSocket criaConexao(BluetoothDevice device){
        Method metodo;
        BluetoothSocket soqueteTmp = null;

        try{
            metodo = device.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
            soqueteTmp = (BluetoothSocket) metodo.invoke(device, 1);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        return soqueteTmp;
    }

    /*
    A partir do soquete de conexão criado será enviado os dados designados
    no dispositivo que foi previamente pareado e selecionado
    após o envio será fechado o soquete.
     */
    private void enviarDado(String dados){
        OutputStream saida;

        if (pareados.size()>0){
            for (BluetoothDevice item : pareados){
                if(dispositivos.getSelectedItem().toString()
                        .equalsIgnoreCase(item.getName())){
                    try {
                        BluetoothDevice dispPar =  bluetoothAdapter.getRemoteDevice(item.getAddress());
                        soquete = criaConexao(dispPar);
                        soquete.connect();

                        Log.i("MAC", item.getAddress());
                        bluetoothAdapter.cancelDiscovery();
                        saida = soquete.getOutputStream();
                        byte[] buffer = dados.getBytes();
                        saida.write(buffer);
                        saida.close();
                        soquete.close();
                        Log.i("Saida",saida.toString());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /*
    Serão capturados os eventos dos botões, exibido as mensagens
    na tela de acordo com o botão pressionado e enviado os dados
    referentes a ligar ou desligar o LED do Arduino
     */
    @Override
    public void onClick(View v) {
        String dado = "0";

        switch (v.getId()){
            case R.id.ligar:
                dado = "1";
                msgTela = Toast.makeText(this, "Ligar Led ",Toast.LENGTH_SHORT);
                msgTela.show();
                Log.i("Dado",dado);
                enviarDado(dado);
                break;

            case R.id.desligar:
                dado = "0";
                msgTela = Toast.makeText(this, "Desligar LED", Toast.LENGTH_SHORT);
                msgTela.show();
                Log.i("Dado",dado);
                enviarDado(dado);
                break;

            default:
                break;
        }
    }
}

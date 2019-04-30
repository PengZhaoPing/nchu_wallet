package com.example.user.nchu_wallet;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.AndroidRuntimeException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.admin.Admin;
import org.web3j.protocol.admin.AdminFactory;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.http.HttpService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.List;

import static android.os.Environment.DIRECTORY_DOWNLOADS;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private String path = null, fileName = null, password = "nchu1234";
    private static final int REQUEST_WRITE_STORAGE_REQUEST_CODE = 112;
    private TextView user_address_textview, user_amount_textview;
    private Button transfer_btn, refresh_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        path = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS).getPath();
        fileName = Memory.getString(this, "ETH_WALLET_FILE_NAME", "null");
        user_address_textview = findViewById(R.id.user_address);
        user_amount_textview = findViewById(R.id.user_amount);
        transfer_btn = findViewById(R.id.transfer_btn);
        refresh_btn = findViewById(R.id.refresh_btn);
        transfer_btn.setOnClickListener(this);
        refresh_btn.setOnClickListener(this);
        requestAppPermissions();
        checkWalletStatus();
    }

    private void checkWalletStatus(){
            try {
                final JSONObject ethkey = new JSONObject(readFromFile(path+"/"+fileName));
                final String address = ethkey.getString("address");
                user_address_textview.setText("0x"+address);
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try  {
                            Admin web3j = AdminFactory.build(new HttpService("https://kovan.infura.io/v3/40f8309fa79c4b7a8ca53e7b26c8c74d"));
                            final BigDecimal balance = new BigDecimal(getTokenBalance(web3j, "0x"+address, "0xd90A06cB48B3BdAc85558D086A3279645acc103E"));
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    user_amount_textview.setText(String.valueOf(balance.divide(BigDecimal.valueOf(1000))));
                                }
                            });

                        } catch (Exception e) {
                            e.printStackTrace();
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    user_amount_textview.setText("0");
                                }
                            });
                        }
                    }
                });
                thread.start();
            } catch (JSONException e) {
                e.printStackTrace();
            }
    }

    private void requestAppPermissions() {
            if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                return;
            }

            ActivityCompat.requestPermissions(this,
                    new String[] {
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    }, REQUEST_WRITE_STORAGE_REQUEST_CODE); // your request code
            checkWalletStatus();
    }

    public static BigInteger getTokenBalance(Web3j web3j, String fromAddress, String contractAddress) {
        String methodName = "balanceOf";
        List<Type> inputParameters = new ArrayList<>();
        List<TypeReference<?>> outputParameters = new ArrayList<>();
        Address address = new Address(fromAddress);
        inputParameters.add(address);

        TypeReference<Uint256> typeReference = new TypeReference<Uint256>() {
        };
        outputParameters.add(typeReference);
        Function function = new Function(methodName, inputParameters, outputParameters);
        String data = FunctionEncoder.encode(function);
        Transaction transaction = Transaction.createEthCallTransaction(fromAddress, contractAddress, data);

        EthCall ethCall;
        BigInteger balanceValue = BigInteger.ZERO;
        try {
            ethCall = web3j.ethCall(transaction, DefaultBlockParameterName.LATEST).send();
            List<Type> results = FunctionReturnDecoder.decode(ethCall.getValue(), function.getOutputParameters());
            balanceValue = (BigInteger) results.get(0).getValue();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return balanceValue;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_WRITE_STORAGE_REQUEST_CODE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(Memory.getString(this, "generateETH_WalletFile", "false").equals("false")) {
                        try {
                            CreateWalletFile();
                        } catch (CipherException e) {
                            e.printStackTrace();
                        } catch (InvalidAlgorithmParameterException e) {
                            e.printStackTrace();
                        } catch (NoSuchAlgorithmException e) {
                            e.printStackTrace();
                        } catch (NoSuchProviderException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Permission denied", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    private void CreateWalletFile() throws CipherException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException, IOException {
        // Create new wallet

        path = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS).getPath();
        fileName = WalletUtils.generateLightNewWalletFile(password, new File(path));
        Memory.setString(this,"ETH_WALLET_FILE_NAME", fileName);
        try {
            final JSONObject ethkey = new JSONObject(readFromFile(path+"/"+fileName));
            final String address = ethkey.getString("address");
            user_address_textview.setText("0x"+address);
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try  {
                        Admin web3j = AdminFactory.build(new HttpService("https://kovan.infura.io/v3/40f8309fa79c4b7a8ca53e7b26c8c74d"));
                        final BigDecimal balance = new BigDecimal(getTokenBalance(web3j, "0x"+address, "0xd90A06cB48B3BdAc85558D086A3279645acc103E"));
                        runOnUiThread(new Runnable() {
                            public void run() {
                                user_amount_textview.setText(String.valueOf(balance.divide(BigDecimal.valueOf(1000))));
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(new Runnable() {
                            public void run() {
                                user_amount_textview.setText(0);
                            }
                        });
                    }
                }
            });
            thread.start();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Memory.setString(this, "generateETH_WalletFile", "true");
        Toast.makeText(this, "創建成功！請至下載區域查看並妥善保管", Toast.LENGTH_LONG).show();

    }

    private  String readFromFile(String path) {
        String ret = "";
        try {
            InputStream inputStream = new FileInputStream(new File(path));

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();
                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString);
                }
                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            Log.e("FileToJson", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("FileToJson", "Can not read file: " + e.toString());
        }
        return ret;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.transfer_btn:
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, TransferActivity.class);
                startActivity(intent);
                finish();
                break;
            case R.id.refresh_btn:
                try {
                    final JSONObject ethkey = new JSONObject(readFromFile(path+"/"+fileName));
                    final String address = ethkey.getString("address");
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try  {
                                Admin web3j = AdminFactory.build(new HttpService("https://kovan.infura.io/v3/40f8309fa79c4b7a8ca53e7b26c8c74d"));
                                final BigDecimal balance = new BigDecimal(getTokenBalance(web3j, "0x"+address, "0xd90A06cB48B3BdAc85558D086A3279645acc103E"));
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        user_amount_textview.setText(String.valueOf(balance.divide(BigDecimal.valueOf(1000))));
                                        Log.d("MainActivity", "onClick: "+balance.divide(BigDecimal.valueOf(1000)));
                                    }
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        user_amount_textview.setText(0);
                                    }
                                });
                            }
                        }
                    });
                    thread.start();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Toast.makeText(this, "刷新成功！", Toast.LENGTH_LONG).show();
                break;
        }
    }
}

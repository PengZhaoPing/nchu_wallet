package com.example.user.nchu_wallet;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.admin.Admin;
import org.web3j.protocol.admin.AdminFactory;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.Contract;
import org.web3j.utils.Numeric;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

import static android.os.Environment.DIRECTORY_DOWNLOADS;


public class TransferActivity extends AppCompatActivity implements View.OnClickListener{
    private EditText transfer_address_edittext, transfer_amount_edittext;
    private Button send_btn;
    private String path = null, fileName = null, password = "nchu1234", txHash = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer);
        transfer_address_edittext = findViewById(R.id.transfer_address_edittext);
        transfer_amount_edittext = findViewById(R.id.transfer_amount_edittext);
        send_btn = findViewById(R.id.send_btn);
        send_btn.setOnClickListener(this);
        path = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS).getPath();
        fileName = Memory.getString(this, "ETH_WALLET_FILE_NAME", "null");
    }

    private String encodeTransferData(String toAddress, BigInteger sum) {
        Function function = new Function(
                "transfer",  // function we're calling
                Arrays.<Type>asList(new Address(toAddress),  new Uint256(sum)),  // Parameters to pass as Solidity Types
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return FunctionEncoder.encode(function);
    }

    private void sendTx(String fromAddress, String toAddress, String walletPassword, String walletFileURL, BigInteger value) throws Exception{
        Admin web3j = AdminFactory.build(new HttpService("https://kovan.infura.io/v3/40f8309fa79c4b7a8ca53e7b26c8c74d"));
        EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(fromAddress, DefaultBlockParameterName.LATEST).sendAsync().get();
        BigInteger nonce = ethGetTransactionCount.getTransactionCount();
        BigInteger realValue = value;
        String encodedFunction = encodeTransferData(toAddress, realValue);
        RawTransaction rawTransaction = RawTransaction.createTransaction(nonce, Contract.GAS_PRICE, Contract.GAS_LIMIT,"0xd90A06cB48B3BdAc85558D086A3279645acc103E", encodedFunction);
        Credentials creds = WalletUtils.loadCredentials(walletPassword, walletFileURL);
        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, creds);
        String hexValue = Numeric.toHexString(signedMessage);
        EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).sendAsync().get();
        final String transactionHash = ethSendTransaction.getTransactionHash();
        Log.d("TransferActivity", "sendTx: "+transactionHash);
        txHash = transactionHash;
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
            case R.id.send_btn:
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final JSONObject ethkey;
                        String myAddress = null;
                        try {
                            ethkey = new JSONObject(readFromFile(path+"/"+fileName));
                            myAddress = ethkey.getString("address");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        BigInteger amount = new BigInteger(transfer_amount_edittext.getText().toString());
                        try {
                            sendTx("0x"+myAddress, transfer_address_edittext.getText().toString(), password, path+"/"+fileName, amount.multiply(BigInteger.valueOf(1000)));
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    Toast.makeText(TransferActivity.this, "交易代碼："+txHash, Toast.LENGTH_LONG).show();
                                }
                            });
                            Intent intent = new Intent();
                            intent.setClass(TransferActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        } catch (Exception e) {
                            Log.d("TransferActivity", "onClick: "+e);
                            e.printStackTrace();
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    Toast.makeText(TransferActivity.this, "交易代碼：(Fail)", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                });
                thread.start();
                break;
        }
    }
}

package dk.edu.mikkel.nfc.activity;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import dk.edu.mikkel.nfc.R;

public class NDEFEraser extends AppCompatActivity {

    private NfcAdapter mAdapter;
    private AlertDialog mDialog;
    private PendingIntent mPendingIntent;
    private Button cleanButton;
    private Button addButton;
    private Button deleteButton;
    private TextView formatText;
    private TextView ndefText;
    private Tag myTag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ndeferaser);

        mDialog = new AlertDialog.Builder(this).setNeutralButton(getString(R.string.general_ok), null).create();

        mAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mAdapter == null) {
            showMessage(getString(R.string.general_error), getString(R.string.general_noSupport));
            finish();
            return;
        }
        mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        cleanButton = (Button) (findViewById(R.id.erase_cleanButton));
        addButton = (Button) (findViewById(R.id.erase_addButton));
        deleteButton = (Button) (findViewById(R.id.erase_deleteButton));
        ndefText = (TextView) (findViewById(R.id.erase_ndefFormated));
        formatText = (TextView) (findViewById(R.id.erase_ndefNotFormated));
    }

    private void showMessage(String title, String message) {
        mDialog.setTitle(title);
        mDialog.setMessage(message);
        mDialog.show();
    }

    private void readFromIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Vibrator v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(100);
            myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (NdefFormatable.get(myTag) != null) {
                addButton.setAlpha(1);
                addButton.setClickable(true);
                formatText.setTextColor(Color.BLACK);
                deleteButton.setAlpha(0.5f);
                deleteButton.setClickable(false);
                cleanButton.setAlpha(0.5f);
                cleanButton.setClickable(false);
                ndefText.setTextColor(Color.LTGRAY);
            } else if (Ndef.get(myTag) != null) {
                addButton.setAlpha(0.5f);
                addButton.setClickable(false);
                formatText.setTextColor(Color.LTGRAY);
                deleteButton.setAlpha(1);
                deleteButton.setClickable(true);
                cleanButton.setAlpha(1);
                cleanButton.setClickable(true);
                ndefText.setTextColor(Color.BLACK);
            } else {
                Toast.makeText(this, R.string.general_noNDEF, Toast.LENGTH_SHORT).show();
                cleanView();
            }
        }
    }

    private void cleanView() {
        addButton.setAlpha(0.5f);
        addButton.setClickable(false);
        formatText.setTextColor(Color.LTGRAY);
        deleteButton.setAlpha(0.5f);
        deleteButton.setClickable(false);
        cleanButton.setAlpha(0.5f);
        cleanButton.setClickable(false);
        ndefText.setTextColor(Color.LTGRAY);
    }

    public void cleanClick(View view) {
        try {
            NdefRecord[] records = new NdefRecord[]{createRecord()};
            NdefMessage message = new NdefMessage(records);
            cleanTag(Ndef.get(myTag), message);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void addClick(View view) {
        try {
            NdefRecord[] records = new NdefRecord[]{createRecord()};
            NdefMessage message = new NdefMessage(records);
            formatTag(NdefFormatable.get(myTag), message);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void deleteClick(View view) {
        deleteFormatting(MifareUltralight.get(myTag));
    }

    private void cleanTag(Ndef ndef, NdefMessage msg) {
        try {
            ndef.connect();

            try {
                ndef.writeNdefMessage(msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                ndef.close();
                Toast.makeText(this, R.string.eraser_cleanSuccess, Toast.LENGTH_SHORT).show();
                cleanView();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void formatTag(NdefFormatable formatable, NdefMessage msg) {
        try {
            formatable.connect();

            try {
                formatable.format(msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                formatable.close();
                Toast.makeText(this, R.string.eraser_formatSuccess, Toast.LENGTH_SHORT).show();
                cleanView();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void deleteFormatting(MifareUltralight mifare) {
        try {
            if (mifare != null) {
                mifare.connect();
                for (int i = 39; i > 3; i--) {
                    mifare.writePage(i, new byte[4]);
                }
                mifare.close();
                Toast.makeText(this, R.string.eraser_deleteSuccess, Toast.LENGTH_SHORT).show();
                cleanView();
            } else {
                Toast.makeText(this, getString(R.string.general_lostConnection), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Toast.makeText(this, getString(R.string.general_lostConnection), Toast.LENGTH_SHORT).show();
        }
    }

    private NdefRecord createRecord() throws UnsupportedEncodingException {
        String lang = "en";
        byte[] textBytes = "".getBytes();
        byte[] langBytes = lang.getBytes("US-ASCII");
        int langLength = langBytes.length;
        int textLength = textBytes.length;
        byte[] payload = new byte[1 + langLength + textLength];
        payload[0] = (byte) langLength;
        System.arraycopy(langBytes, 0, payload, 1, langLength);
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);

        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
                NdefRecord.RTD_TEXT, new byte[0], payload);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        readFromIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAdapter != null) {
            if (!mAdapter.isEnabled()) {
                showWirelessSettingsDialog();
            }
            mAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAdapter != null) {
            mAdapter.disableForegroundDispatch(this);
        }
    }

    private void showWirelessSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.general_nfcNotEnabled));
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                startActivity(intent);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });
        builder.create().show();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            ImageView img = (ImageView) findViewById(R.id.erase_searching);
            AnimationDrawable frameAnimation = (AnimationDrawable) img.getDrawable();
            frameAnimation.start();
        }
    }

    public void clickTag(View view) {
        ImageView img = (ImageView) findViewById(R.id.erase_searching);
        AnimationDrawable frameAnimation = (AnimationDrawable) img.getDrawable();
        if (frameAnimation.isRunning()) {
            frameAnimation.stop();
        } else {
            frameAnimation.start();
        }
    }
}

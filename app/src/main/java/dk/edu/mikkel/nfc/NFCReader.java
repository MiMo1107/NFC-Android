package dk.edu.mikkel.nfc;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.NfcA;
import android.os.Parcelable;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import dk.edu.mikkel.nfc.record.ParsedNdefRecord;

public class NFCReader extends AppCompatActivity {

    private static final DateFormat TIME_FORMAT = SimpleDateFormat.getDateTimeInstance();
    private LinearLayout mTagContent;
    private PendingIntent mPendingIntent;
    private NfcAdapter mAdapter;
    private AlertDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfcreader);
        nfcIntent(getIntent());
        mTagContent = (LinearLayout) findViewById(R.id.list);

        mDialog = new AlertDialog.Builder(this).setNeutralButton("Ok", null).create();

        mAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mAdapter == null) {
            showMessage("Error", "This device does not support NFC");
            finish();
            return;
        }

        mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

    }

    private void showMessage(String title, String message) {
        mDialog.setTitle(title);
        mDialog.setMessage(message);
        mDialog.show();
    }

    private void nfcIntent(Intent intent) {
        String action = intent.getAction();

        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {

            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            TextView textView = new TextView(getBaseContext());
            NdefMessage[] msgs = null;
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }

            }
                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

                textView.append("\nTag Id1: "  + tag.getId());
                textView.append("\nTag Id2: "  + intent.getByteArrayExtra(NfcAdapter.EXTRA_ID));
                textView.append("\nTag Describe contents: "  + tag.describeContents());
                String tech = "";
                for (int i = 0; i < tag.getTechList().length; i++) {
                    tech += "\n#" + i + ": " + tag.getTechList()[i] + "; ";
                }
                textView.append("\nTech: "  + tech);
                NfcA nfcA = NfcA.get(tag);
                if(nfcA != null) {
                    String atqa = "";
                    for (int i = 0; i < nfcA.getAtqa().length; i++) {
                        atqa += "#" + i + ": " + nfcA.getAtqa()[i] + "; ";
                    }
                    textView.append("\nNfcA Atqa: " + atqa);
                    textView.append("\nNfcA Sak: " + nfcA.getSak());
                    textView.append("\nNfcA MAX: " + nfcA.getMaxTransceiveLength());
                }
                IsoDep iso = IsoDep.get(tag);
                String is = "";
                if(iso != null) {
                    for (int i = 0; i < iso.getHistoricalBytes().length; i++) {
                        is += "#" + i + ": " + iso.getHistoricalBytes()[i] + "; ";
                    }
                    textView.append("\nIso hist: "  + is);
                }
            buildTagViews(msgs);
        }
    }

    void buildTagViews(NdefMessage[] msgs) {
        if (msgs == null || msgs.length == 0) {
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(this);
        LinearLayout content = mTagContent;

        // Parse the first message in the list
        // Build views for all of the sub records
        Date now = new Date();
        List<ParsedNdefRecord> records = NdefMessageParser.parse(msgs[0]);
        final int size = records.size();
        for (int i = 0; i < size; i++) {
            TextView timeView = new TextView(this);
            timeView.setText(TIME_FORMAT.format(now));
            content.addView(timeView, 0);
            ParsedNdefRecord record = records.get(i);
            content.addView(record.getView(this, inflater, content, i), 1 + i);
            content.addView(inflater.inflate(R.layout.tag_divider, content, false), 2 + i);
        }
    }

    private void nfcSimpleIntent(Intent intent) {
        String action = intent.getAction();

        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {

            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            byte[] id = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
            Parcelable[] raw =
                    intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

        }
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
        builder.setMessage("NFC is not enabled. Please go to the wireless settings to enable it.");
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
        return;
    }

    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);
        nfcIntent(intent);
    }
}

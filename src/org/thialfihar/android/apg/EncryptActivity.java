/*
 * Copyright (C) 2010 Thialfihar <thi@thialfihar.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.thialfihar.android.apg;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.util.Vector;

import org.bouncycastle2.openpgp.PGPException;
import org.bouncycastle2.openpgp.PGPPublicKey;
import org.bouncycastle2.openpgp.PGPPublicKeyRing;
import org.bouncycastle2.openpgp.PGPSecretKey;
import org.bouncycastle2.openpgp.PGPSecretKeyRing;
import org.bouncycastle2.util.Strings;
import org.thialfihar.android.apg.Apg.GeneralException;
import org.thialfihar.android.apg.utils.Choice;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.text.ClipboardManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

public class EncryptActivity extends BaseActivity {
    private Intent mIntent = null;
    private String mSubject = null;
    private String mSendTo = null;

    private long mEncryptionKeyIds[] = null;

    private boolean mReturnResult = false;
    private EditText mMessage = null;
    private Button mSelectKeysButton = null;
    private Button mEncryptButton = null;
    private Button mEncryptToClipboardButton = null;
    private CheckBox mSign = null;
    private TextView mMainUserId = null;
    private TextView mMainUserIdRest = null;

    private ViewFlipper mSource = null;
    private TextView mSourceLabel = null;
    private ImageView mSourcePrevious = null;
    private ImageView mSourceNext = null;

    private ViewFlipper mMode = null;
    private TextView mModeLabel = null;
    private ImageView mModePrevious = null;
    private ImageView mModeNext = null;

    private int mEncryptTarget;

    private EditText mPassPhrase = null;
    private EditText mPassPhraseAgain = null;
    private CheckBox mAsciiArmour = null;
    private Spinner mFileCompression = null;

    private EditText mFilename = null;
    private CheckBox mDeleteAfter = null;
    private ImageButton mBrowse = null;

    private String mInputFilename = null;
    private String mOutputFilename = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.encrypt);

        mSource = (ViewFlipper) findViewById(R.id.source);
        mSourceLabel = (TextView) findViewById(R.id.sourceLabel);
        mSourcePrevious = (ImageView) findViewById(R.id.sourcePrevious);
        mSourceNext = (ImageView) findViewById(R.id.sourceNext);

        mSourcePrevious.setClickable(true);
        mSourcePrevious.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mSource.setInAnimation(AnimationUtils.loadAnimation(EncryptActivity.this,
                                                                    R.anim.push_right_in));
                mSource.setOutAnimation(AnimationUtils.loadAnimation(EncryptActivity.this,
                                                                     R.anim.push_right_out));
                mSource.showPrevious();
                updateSource();
            }
        });

        mSourceNext.setClickable(true);
        OnClickListener nextSourceClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                mSource.setInAnimation(AnimationUtils.loadAnimation(EncryptActivity.this,
                                                                    R.anim.push_left_in));
                mSource.setOutAnimation(AnimationUtils.loadAnimation(EncryptActivity.this,
                                                                     R.anim.push_left_out));
                mSource.showNext();
                updateSource();
            }
        };
        mSourceNext.setOnClickListener(nextSourceClickListener);

        mSourceLabel.setClickable(true);
        mSourceLabel.setOnClickListener(nextSourceClickListener);

        mMode = (ViewFlipper) findViewById(R.id.mode);
        mModeLabel = (TextView) findViewById(R.id.modeLabel);
        mModePrevious = (ImageView) findViewById(R.id.modePrevious);
        mModeNext = (ImageView) findViewById(R.id.modeNext);

        mModePrevious.setClickable(true);
        mModePrevious.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mMode.setInAnimation(AnimationUtils.loadAnimation(EncryptActivity.this,
                                                                    R.anim.push_right_in));
                mMode.setOutAnimation(AnimationUtils.loadAnimation(EncryptActivity.this,
                                                                     R.anim.push_right_out));
                mMode.showPrevious();
                updateMode();
            }
        });

        OnClickListener nextModeClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                mMode.setInAnimation(AnimationUtils.loadAnimation(EncryptActivity.this,
                                                                    R.anim.push_left_in));
                mMode.setOutAnimation(AnimationUtils.loadAnimation(EncryptActivity.this,
                                                                     R.anim.push_left_out));
                mMode.showNext();
                updateMode();
            }
        };
        mModeNext.setOnClickListener(nextModeClickListener);

        mModeLabel.setClickable(true);
        mModeLabel.setOnClickListener(nextModeClickListener);

        mMessage = (EditText) findViewById(R.id.message);
        mSelectKeysButton = (Button) findViewById(R.id.btn_selectEncryptKeys);
        mEncryptButton = (Button) findViewById(R.id.btn_encrypt);
        mEncryptToClipboardButton = (Button) findViewById(R.id.btn_encryptToClipboard);
        mSign = (CheckBox) findViewById(R.id.sign);
        mMainUserId = (TextView) findViewById(R.id.mainUserId);
        mMainUserIdRest = (TextView) findViewById(R.id.mainUserIdRest);

        mPassPhrase = (EditText) findViewById(R.id.passPhrase);
        mPassPhraseAgain = (EditText) findViewById(R.id.passPhraseAgain);

        // measure the height of the source_file view and set the message view's min height to that,
        // so it fills mSource fully... bit of a hack.
        View tmp = findViewById(R.id.sourceFile);
        tmp.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int height = tmp.getMeasuredHeight();
        mMessage.setMinimumHeight(height);

        mFilename = (EditText) findViewById(R.id.filename);
        mBrowse = (ImageButton) findViewById(R.id.btn_browse);
        mBrowse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFile();
            }
        });

        mFileCompression = (Spinner) findViewById(R.id.fileCompression);
        Choice[] choices = new Choice[] {
                new Choice(Id.choice.compression.none, getString(R.string.choice_none)),
                new Choice(Id.choice.compression.zip, "ZIP"),
                new Choice(Id.choice.compression.bzip2, "BZIP2"),
                new Choice(Id.choice.compression.zlib, "ZLIB"),
        };
        ArrayAdapter<Choice> adapter =
                new ArrayAdapter<Choice>(this, android.R.layout.simple_spinner_item, choices);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mFileCompression.setAdapter(adapter);

        int defaultFileCompression = getDefaultFileCompression();
        for (int i = 0; i < choices.length; ++i) {
            if (choices[i].getId() == defaultFileCompression) {
                mFileCompression.setSelection(i);
                break;
            }
        }

        mDeleteAfter = (CheckBox) findViewById(R.id.deleteAfterEncryption);

        mAsciiArmour = (CheckBox) findViewById(R.id.asciiArmour);
        mAsciiArmour.setChecked(getDefaultAsciiArmour());
        mAsciiArmour.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                guessOutputFilename();
            }
        });

        mEncryptToClipboardButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                encryptToClipboardClicked();
            }
        });

        mEncryptButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                encryptClicked();
            }
        });

        mSelectKeysButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                selectPublicKeys();
            }
        });

        mSign.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckBox checkBox = (CheckBox) v;
                if (checkBox.isChecked()) {
                    selectSecretKey();
                } else {
                    setSecretKeyId(Id.key.none);
                    updateView();
                }
            }
        });

        mIntent = getIntent();
        if (Apg.Intent.ENCRYPT.equals(mIntent.getAction()) ||
            Apg.Intent.ENCRYPT_FILE.equals(mIntent.getAction()) ||
            Apg.Intent.ENCRYPT_AND_RETURN.equals(mIntent.getAction())) {
            Bundle extras = mIntent.getExtras();
            if (extras == null) {
                extras = new Bundle();
            }

            if (Apg.Intent.ENCRYPT_AND_RETURN.equals(mIntent.getAction())) {
                mReturnResult = true;
            }

            String data = extras.getString(Apg.EXTRA_TEXT);
            mSendTo = extras.getString(Apg.EXTRA_SEND_TO);
            mSubject = extras.getString(Apg.EXTRA_SUBJECT);
            long signatureKeyId = extras.getLong(Apg.EXTRA_SIGNATURE_KEY_ID);
            long encryptionKeyIds[] = extras.getLongArray(Apg.EXTRA_ENCRYPTION_KEY_IDS);
            if (signatureKeyId != 0) {
                PGPSecretKeyRing keyRing = Apg.getSecretKeyRing(signatureKeyId);
                PGPSecretKey masterKey = null;
                if (keyRing != null) {
                    masterKey = Apg.getMasterKey(keyRing);
                    if (masterKey != null) {
                        Vector<PGPSecretKey> signKeys = Apg.getUsableSigningKeys(keyRing);
                        if (signKeys.size() > 0) {
                            setSecretKeyId(masterKey.getKeyID());
                        }
                    }
                }
            }

            if (encryptionKeyIds != null) {
                Vector<Long> goodIds = new Vector<Long>();
                for (int i = 0; i < encryptionKeyIds.length; ++i) {
                    PGPPublicKeyRing keyRing = Apg.getPublicKeyRing(encryptionKeyIds[i]);
                    PGPPublicKey masterKey = null;
                    if (keyRing == null) {
                        continue;
                    }
                    masterKey = Apg.getMasterKey(keyRing);
                    if (masterKey == null) {
                        continue;
                    }
                    Vector<PGPPublicKey> encryptKeys = Apg.getUsableEncryptKeys(keyRing);
                    if (encryptKeys.size() == 0) {
                        continue;
                    }
                    goodIds.add(masterKey.getKeyID());
                }
                if (goodIds.size() > 0) {
                    mEncryptionKeyIds = new long[goodIds.size()];
                    for (int i = 0; i < goodIds.size(); ++i) {
                        mEncryptionKeyIds[i] = goodIds.get(i);
                    }
                }
            }

            if (Apg.Intent.ENCRYPT.equals(mIntent.getAction()) ||
                Apg.Intent.ENCRYPT_AND_RETURN.equals(mIntent.getAction())) {
                if (data != null) {
                    mMessage.setText(data);
                }
                mSource.setInAnimation(null);
                mSource.setOutAnimation(null);
                while (mSource.getCurrentView().getId() != R.id.sourceMessage) {
                    mSource.showNext();
                }
            } else if (Apg.Intent.ENCRYPT_FILE.equals(mIntent.getAction())) {
                if ("file".equals(mIntent.getScheme())) {
                    mInputFilename = mIntent.getDataString().replace("file://", "");
                    mFilename.setText(mInputFilename);
                    guessOutputFilename();
                }
                mSource.setInAnimation(null);
                mSource.setOutAnimation(null);
                while (mSource.getCurrentView().getId() != R.id.sourceFile) {
                    mSource.showNext();
                }
            }
        }

        updateView();
        updateSource();
        updateMode();

        if (mReturnResult) {
            mSourcePrevious.setClickable(false);
            mSourcePrevious.setEnabled(false);
            mSourcePrevious.setVisibility(View.INVISIBLE);

            mSourceNext.setClickable(false);
            mSourceNext.setEnabled(false);
            mSourceNext.setVisibility(View.INVISIBLE);

            mSourceLabel.setClickable(false);
            mSourceLabel.setEnabled(false);

            mEncryptToClipboardButton.setEnabled(false);
            mEncryptToClipboardButton.setVisibility(View.INVISIBLE);
            mEncryptButton.setText(R.string.btn_encrypt);
        }

        if (mReturnResult &&
            mMessage.getText().length() > 0 &&
            ((mEncryptionKeyIds != null &&
              mEncryptionKeyIds.length > 0) ||
             getSecretKeyId() != 0)) {
            encryptClicked();
        }
    }

    private void openFile() {
        String filename = mFilename.getText().toString();

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        intent.setData(Uri.parse("file://" + filename));
        intent.setType("*/*");

        try {
            startActivityForResult(intent, Id.request.filename);
        } catch (ActivityNotFoundException e) {
            // No compatible file manager was found.
            Toast.makeText(this, R.string.noFilemanagerInstalled, Toast.LENGTH_SHORT).show();
        }
    }

    private void guessOutputFilename() {
        mInputFilename = mFilename.getText().toString();
        File file = new File(mInputFilename);
        String ending = (mAsciiArmour.isChecked() ? ".asc" : ".gpg");
        mOutputFilename = Constants.path.app_dir + "/" + file.getName() + ending;
    }

    private void updateSource() {
        switch (mSource.getCurrentView().getId()) {
            case R.id.sourceFile: {
                mSourceLabel.setText(R.string.label_file);
                mEncryptButton.setText(R.string.btn_encrypt);
                mEncryptToClipboardButton.setEnabled(false);
                mEncryptToClipboardButton.setVisibility(View.INVISIBLE);
                break;
            }

            case R.id.sourceMessage: {
                mSourceLabel.setText(R.string.label_message);
                mEncryptButton.setText(R.string.btn_send);
                mEncryptToClipboardButton.setEnabled(true);
                mEncryptToClipboardButton.setVisibility(View.VISIBLE);
                break;
            }

            default: {
                break;
            }
        }
    }

    private void updateMode() {
        switch (mMode.getCurrentView().getId()) {
            case R.id.modeAsymmetric: {
                mModeLabel.setText(R.string.label_asymmetric);
                break;
            }

            case R.id.modeSymmetric: {
                mModeLabel.setText(R.string.label_symmetric);
                break;
            }

            default: {
                break;
            }
        }
    }

    private void encryptToClipboardClicked() {
        mEncryptTarget = Id.target.clipboard;
        initiateEncryption();
    }

    private void encryptClicked() {
        if (mSource.getCurrentView().getId() == R.id.sourceFile) {
            mEncryptTarget = Id.target.file;
        } else {
            mEncryptTarget = Id.target.email;
        }
        initiateEncryption();
    }

    private void initiateEncryption() {
        if (mEncryptTarget == Id.target.file) {
            String currentFilename = mFilename.getText().toString();
            if (mInputFilename == null || !mInputFilename.equals(currentFilename)) {
                guessOutputFilename();
            }

            if (mInputFilename.equals("")) {
                Toast.makeText(this, R.string.noFileSelected, Toast.LENGTH_SHORT).show();
                return;
            }

            File file = new File(mInputFilename);
            if (!file.exists() || !file.isFile()) {
                Toast.makeText(this, getString(R.string.errorMessage,
                                               getString(R.string.error_fileNotFound)),
                               Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // symmetric encryption
        if (mMode.getCurrentView().getId() == R.id.modeSymmetric) {
            boolean gotPassPhrase = false;
            String passPhrase = mPassPhrase.getText().toString();
            String passPhraseAgain = mPassPhraseAgain.getText().toString();
            if (!passPhrase.equals(passPhraseAgain)) {
                Toast.makeText(this, R.string.passPhrasesDoNotMatch, Toast.LENGTH_SHORT).show();
                return;
            }

            gotPassPhrase = (passPhrase.length() != 0);
            if (!gotPassPhrase) {
                Toast.makeText(this, R.string.passPhraseMustNotBeEmpty, Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            boolean encryptIt = (mEncryptionKeyIds != null && mEncryptionKeyIds.length > 0);
            // for now require at least one form of encryption for files
            if (!encryptIt && mEncryptTarget == Id.target.file) {
                Toast.makeText(this, R.string.selectEncryptionKey, Toast.LENGTH_SHORT).show();
                return;
            }

            if (!encryptIt && getSecretKeyId() == 0) {
                Toast.makeText(this, R.string.selectEncryptionOrSignatureKey,
                               Toast.LENGTH_SHORT).show();
                return;
            }

            if (getSecretKeyId() != 0 && Apg.getCachedPassPhrase(getSecretKeyId()) == null) {
                showDialog(Id.dialog.pass_phrase);
                return;
            }
        }

        if (mEncryptTarget == Id.target.file) {
            askForOutputFilename();
        } else {
            encryptStart();
        }
    }

    private void askForOutputFilename() {
        showDialog(Id.dialog.output_filename);
    }

    @Override
    public void passPhraseCallback(long keyId, String passPhrase) {
        super.passPhraseCallback(keyId, passPhrase);
        if (mEncryptTarget == Id.target.file) {
            askForOutputFilename();
        } else {
            encryptStart();
        }
    }

    private void encryptStart() {
        showDialog(Id.dialog.encrypting);
        startThread();
    }

    @Override
    public void run() {
        String error = null;
        Bundle data = new Bundle();
        Message msg = new Message();

        try {
            InputStream in;
            OutputStream out;
            long size;
            boolean useAsciiArmour = true;
            long encryptionKeyIds[] = null;
            long signatureKeyId = 0;
            boolean signOnly = false;
            int compressionId = 0;

            String passPhrase = null;
            if (mMode.getCurrentView().getId() == R.id.modeSymmetric) {
                passPhrase = mPassPhrase.getText().toString();
                if (passPhrase.length() == 0) {
                    passPhrase = null;
                }
            } else {
                encryptionKeyIds = mEncryptionKeyIds;
                signatureKeyId = getSecretKeyId();
                signOnly = (mEncryptionKeyIds == null || mEncryptionKeyIds.length == 0);
            }

            if (mEncryptTarget == Id.target.file) {
                if (mInputFilename.startsWith(Environment.getExternalStorageDirectory().getAbsolutePath()) ||
                    mOutputFilename.startsWith(Environment.getExternalStorageDirectory().getAbsolutePath())) {
                    if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                        throw new GeneralException(getString(R.string.error_externalStorageNotReady));
                    }
                }

                in = new FileInputStream(mInputFilename);
                out = new FileOutputStream(mOutputFilename);

                File file = new File(mInputFilename);
                size = file.length();
                useAsciiArmour = mAsciiArmour.isChecked();
                compressionId = ((Choice) mFileCompression.getSelectedItem()).getId();
            } else {
                String message = mMessage.getText().toString();

                if (signOnly && mReturnResult) {
                    // fix the message a bit, trailing spaces and newlines break stuff,
                    // because GMail sends as HTML and such things fuck up the signature,
                    // TODO: things like "<" and ">" also fuck up the signature
                    message = message.replaceAll(" +\n", "\n");
                    message = message.replaceAll("\n\n+", "\n\n");
                    message = message.replaceFirst("^\n+", "");
                    // make sure there'll be exactly one newline at the end
                    message = message.replaceFirst("\n*$", "\n");
                }

                byte[] byteData = Strings.toUTF8ByteArray(message);
                in = new ByteArrayInputStream(byteData);
                out = new ByteArrayOutputStream();

                size = byteData.length;
                useAsciiArmour = true;
                compressionId = getDefaultMessageCompression();
            }

            if (signOnly) {
                Apg.signText(this, in, out, getSecretKeyId(),
                             Apg.getCachedPassPhrase(getSecretKeyId()),
                             getDefaultHashAlgorithm(), this);
            } else {
                Apg.encrypt(this, in, out, size, useAsciiArmour,
                            encryptionKeyIds, signatureKeyId,
                            Apg.getCachedPassPhrase(signatureKeyId), this,
                            getDefaultEncryptionAlgorithm(), getDefaultHashAlgorithm(),
                            compressionId, passPhrase);
            }

            out.close();
            if (mEncryptTarget != Id.target.file) {
                data.putString(Apg.EXTRA_ENCRYPTED_MESSAGE,
                               new String(((ByteArrayOutputStream)out).toByteArray()));
            }
        } catch (IOException e) {
            error = "" + e;
        } catch (PGPException e) {
            error = "" + e;
        } catch (NoSuchProviderException e) {
            error = "" + e;
        } catch (NoSuchAlgorithmException e) {
            error = "" + e;
        } catch (SignatureException e) {
            error = "" + e;
        } catch (Apg.GeneralException e) {
            error = "" + e;
        }

        data.putInt(Apg.EXTRA_STATUS, Id.message.done);

        if (error != null) {
            data.putString(Apg.EXTRA_ERROR, error);
        }

        msg.setData(data);
        sendMessage(msg);
    }

    private void updateView() {
        if (mEncryptionKeyIds == null || mEncryptionKeyIds.length == 0) {
            mSelectKeysButton.setText(R.string.noKeysSelected);
        } else if (mEncryptionKeyIds.length == 1) {
            mSelectKeysButton.setText(R.string.oneKeySelected);
        } else {
            mSelectKeysButton.setText("" + mEncryptionKeyIds.length + " " +
                                      getResources().getString(R.string.nKeysSelected));
        }

        if (getSecretKeyId() == 0) {
            mSign.setChecked(false);
            mMainUserId.setText("");
            mMainUserIdRest.setText("");
        } else {
            String uid = getResources().getString(R.string.unknownUserId);
            String uidExtra = "";
            PGPSecretKeyRing keyRing = Apg.getSecretKeyRing(getSecretKeyId());
            if (keyRing != null) {
                PGPSecretKey key = Apg.getMasterKey(keyRing);
                if (key != null) {
                    String userId = Apg.getMainUserIdSafe(this, key);
                    String chunks[] = userId.split(" <", 2);
                    uid = chunks[0];
                    if (chunks.length > 1) {
                        uidExtra = "<" + chunks[1];
                    }
                }
            }
            mMainUserId.setText(uid);
            mMainUserIdRest.setText(uidExtra);
            mSign.setChecked(true);
        }
    }

    private void selectPublicKeys() {
        Intent intent = new Intent(this, SelectPublicKeyListActivity.class);
        intent.putExtra(Apg.EXTRA_SELECTION, mEncryptionKeyIds);
        startActivityForResult(intent, Id.request.public_keys);
    }

    private void selectSecretKey() {
        Intent intent = new Intent(this, SelectSecretKeyListActivity.class);
        startActivityForResult(intent, Id.request.secret_keys);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Id.request.filename: {
                if (resultCode == RESULT_OK && data != null) {
                    String filename = data.getDataString();
                    if (filename != null) {
                        // Get rid of URI prefix:
                        if (filename.startsWith("file://")) {
                            filename = filename.substring(7);
                        }
                        // replace %20 and so on
                        filename = Uri.decode(filename);

                        mFilename.setText(filename);
                    }
                }
                return;
            }

            case Id.request.output_filename: {
                if (resultCode == RESULT_OK && data != null) {
                    String filename = data.getDataString();
                    if (filename != null) {
                        // Get rid of URI prefix:
                        if (filename.startsWith("file://")) {
                            filename = filename.substring(7);
                        }
                        // replace %20 and so on
                        filename = Uri.decode(filename);

                        FileDialog.setFilename(filename);
                    }
                }
                return;
            }

            case Id.request.secret_keys: {
                if (resultCode == RESULT_OK) {
                    super.onActivityResult(requestCode, resultCode, data);
                }
                updateView();
                break;
            }

            case Id.request.public_keys: {
                if (resultCode == RESULT_OK) {
                    Bundle bundle = data.getExtras();
                    mEncryptionKeyIds = bundle.getLongArray(Apg.EXTRA_SELECTION);
                }
                updateView();
                break;
            }

            default: {
                break;
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void doneCallback(Message msg) {
        super.doneCallback(msg);

        removeDialog(Id.dialog.encrypting);

        Bundle data = msg.getData();
        String error = data.getString(Apg.EXTRA_ERROR);
        if (error != null) {
            Toast.makeText(EncryptActivity.this,
                           getString(R.string.errorMessage, error), Toast.LENGTH_SHORT).show();
            return;
        }
        switch (mEncryptTarget) {
            case Id.target.clipboard: {
                String message = data.getString(Apg.EXTRA_ENCRYPTED_MESSAGE);
                ClipboardManager clip = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                clip.setText(message);
                Toast.makeText(this, R.string.encryptionToClipboardSuccessful,
                               Toast.LENGTH_SHORT).show();
                break;
            }

            case Id.target.email: {
                if (mReturnResult) {
                    Intent intent = new Intent();
                    intent.putExtras(data);
                    setResult(RESULT_OK, intent);
                    finish();
                    return;
                }

                String message = data.getString(Apg.EXTRA_ENCRYPTED_MESSAGE);
                Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
                emailIntent.setType("text/plain; charset=utf-8");
                emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, message);
                if (mSubject != null) {
                    emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                                         mSubject);
                }
                if (mSendTo != null) {
                    emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,
                                         new String[] { mSendTo });
                }
                EncryptActivity.this.
                        startActivity(Intent.createChooser(emailIntent,
                                                           getString(R.string.title_sendEmail)));
                break;
            }

            case Id.target.file: {
                Toast.makeText(this, R.string.encryptionSuccessful, Toast.LENGTH_SHORT).show();
                if (mDeleteAfter.isChecked()) {
                    setDeleteFile(mInputFilename);
                    showDialog(Id.dialog.delete_file);
                }
                break;
            }

            default: {
                // shouldn't happen
                break;
            }
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case Id.dialog.output_filename: {
                return FileDialog.build(this, getString(R.string.title_encryptToFile),
                                        getString(R.string.specifyFileToEncryptTo),
                                        mOutputFilename,
                                        new FileDialog.OnClickListener() {

                                            @Override
                                            public void onOkClick(String filename) {
                                                removeDialog(Id.dialog.output_filename);
                                                mOutputFilename = filename;
                                                encryptStart();
                                            }

                                            @Override
                                            public void onCancelClick() {
                                                removeDialog(Id.dialog.output_filename);
                                            }
                                        },
                                        getString(R.string.filemanager_titleSave),
                                        getString(R.string.filemanager_btnSave),
                                        Id.request.output_filename);
            }

            default: {
                break;
            }
        }

        return super.onCreateDialog(id);
    }
}
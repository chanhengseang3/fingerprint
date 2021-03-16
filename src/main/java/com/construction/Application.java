package com.construction;

import com.construction.db.DbConnector;
import com.construction.db.SubConstructor;
import com.construction.service.ServerSyncService;
import com.construction.utils.StringUtil;
import com.zkteco.biometric.FingerprintSensorErrorCode;
import com.zkteco.biometric.FingerprintSensorEx;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.construction.utils.BitmapUtil.byteArrayToInt;
import static com.construction.utils.BitmapUtil.writeBitmap;

public class Application extends JFrame {

    private static final int nFakeFunOn = 1;
    private static final DbConnector DB = new DbConnector();
    //for verify test
    private final byte[] lastRegTemp = new byte[2048];
    //pre-register template
    private final byte[][] regTempArray = new byte[3][2048];
    private final byte[] template = new byte[2048];
    private final int[] templateLen = new int[1];
    JButton btnOpen = null;
    JButton btnEnroll = null;
    JButton btnIdentify = null;
    JButton btnSync = null;
    JButton btnClose = null;
    JButton btnClear = null;
    JButton btnImg = null;
    JLabel idLabel = null;
    JTextField idField = null;
    //the width of fingerprint image
    int fpWidth = 0;
    //the height of fingerprint image
    int fpHeight = 0;
    private JTextArea textArea;
    //the length of lastRegTemp
    private int cbRegTemp = 0;
    //Register
    private boolean bRegister = false;
    //Identify
    private boolean bIdentify = true;

    //the index of pre-register function
    private int enroll_idx = 0;
    private byte[] imgBuf = null;
    private boolean mbStop = true;
    private long mhDevice = 0;
    private long mhDB = 0;
    private WorkThread workThread = null;

    public static void main(String[] args) {
        new Application().launchFrame();
    }

    public void launchFrame() {
        this.setLayout(null);
        btnOpen = new JButton("Open");
        this.add(btnOpen);
        int nRSize = 20;
        btnOpen.setBounds(30, 10 + nRSize, 100, 30);

        btnEnroll = new JButton("Enroll");
        this.add(btnEnroll);
        btnEnroll.setBounds(30, 60 + nRSize, 100, 30);

        btnIdentify = new JButton("Identify");
        this.add(btnIdentify);
        btnIdentify.setBounds(30, 110 + nRSize, 100, 30);

        idLabel = new JLabel("User ID:");
        this.add(idLabel);
        idLabel.setBounds(30, 160 + nRSize, 100, 30);

        idField = new JTextField();
        this.add(idField);
        idField.setBounds(30, 190 + nRSize, 100, 30);

        btnSync = new JButton("Sync Data");
        this.add(btnSync);
        btnSync.setBounds(30, 240 + nRSize, 100, 30);

        btnClose = new JButton("Close");
        this.add(btnClose);
        btnClose.setBounds(30, 300 + nRSize, 100, 30);

        btnClear = new JButton("Clear DB");
        this.add(btnClear);
        btnClear.setBounds(30, 360 + nRSize, 100, 30);

        btnImg = new JButton();
        btnImg.setBounds(160, 30, 280, 400);
        btnImg.setDefaultCapable(false);
        this.add(btnImg);

        textArea = new JTextArea();
        this.add(textArea);
        textArea.setBounds(10, 440, 480, 100);

        this.setSize(520, 600);
        this.setLocationRelativeTo(null);
        this.setVisible(true);
        this.setTitle("Construction System");
        this.setResizable(false);

        btnOpen.addActionListener(e -> {
            if (0 != mhDevice) {
                // already initiated
                textArea.setText("Please close device first!");
                return;
            }
            int ret;
            //Initialize
            cbRegTemp = 0;
            bRegister = false;
            bIdentify = false;
            enroll_idx = 0;
            if (FingerprintSensorErrorCode.ZKFP_ERR_OK != FingerprintSensorEx.Init()) {
                textArea.setText("Init failed!");
                return;
            }
            ret = FingerprintSensorEx.GetDeviceCount();
            if (ret < 0) {
                textArea.setText("No devices connected!");
                FreeSensor();
                return;
            }
            if (0 == (mhDevice = FingerprintSensorEx.OpenDevice(0))) {
                textArea.setText("Open device fail, ret = " + ret + "!");
                FreeSensor();
                return;
            }
            if (0 == (mhDB = FingerprintSensorEx.DBInit())) {
                textArea.setText("Init DB fail, ret = " + ret + "!");
                FreeSensor();
                return;
            } else { // load data from db to finger print
                textArea.setText("loading data from db to finger print...");
                int len = 2048;
                byte[] temp = new byte[len];
                List<SubConstructor> subConstructors = DB.getAll();
                for (SubConstructor subConstructor : subConstructors) {
                    FingerprintSensorEx.Base64ToBlob(subConstructor.getBase64(), temp, len);
                    FingerprintSensorEx.DBAdd(mhDB, subConstructor.getId(), temp);
                }
                textArea.setText("finish loading data");
            }

            //FingerprintSensorEx.SetParameter(mhDevice, 2002, changeByte(nFakeFunOn), 4);
            byte[] paramValue = new byte[4];
            int[] size = new int[1];

            //GetFakeOn
            //size[0] = 4;
            //FingerprintSensorEx.GetParameters(mhDevice, 2002, paramValue, size);
            //nFakeFunOn = byteArrayToInt(paramValue);
            size[0] = 4;

            FingerprintSensorEx.GetParameters(mhDevice, 1, paramValue, size);
            fpWidth = byteArrayToInt(paramValue);

            size[0] = 4;
            FingerprintSensorEx.GetParameters(mhDevice, 2, paramValue, size);
            fpHeight = byteArrayToInt(paramValue);

            //width = fingerprintSensor.getImageWidth();
            //height = fingerprintSensor.getImageHeight();
            imgBuf = new byte[fpWidth * fpHeight];
            btnImg.setSize(fpWidth, fpHeight);
            mbStop = false;
            workThread = new WorkThread();
            workThread.start();
            textArea.setText("Open success!");
        });

        btnClose.addActionListener(e -> {
            FreeSensor();
            textArea.setText("Close success!");
        });

        btnEnroll.addActionListener(e -> {
            if (0 == mhDevice) {
                textArea.setText("Please open device first!");
                return;
            }
            if (!bRegister) {
                enroll_idx = 0;
                bRegister = true;
                textArea.setText("Please your finger 3 times!");
            }
        });

        btnSync.addActionListener(e -> {
            textArea.setText("synchronizing data with server");
        });

        btnClear.addActionListener(e -> {
            if (0 == mhDevice) {
                textArea.setText("Please open device first!");
                return;
            }
            int select = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to clear DB?",
                    "Warning",
                    JOptionPane.OK_CANCEL_OPTION);
            if (select == 2) {
                return;
            }
            textArea.setText("Delete all data in DB");
            DB.deleteAll();
            FingerprintSensorEx.DBFree(mhDB);
            mhDB = FingerprintSensorEx.DBInit();
            textArea.setText("DB has been cleared");
        });

        btnIdentify.addActionListener(e -> {
            if (0 == mhDevice) {
                textArea.setText("Please Open device first!");
                return;
            }
            if (bRegister) {
                enroll_idx = 0;
                bRegister = false;
            }
            if (!bIdentify) {
                bIdentify = true;
            }
        });

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                FreeSensor();
                DB.close();
            }
        });
    }

    private void FreeSensor() {
        mbStop = true;
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (0 != mhDB) {
            FingerprintSensorEx.DBFree(mhDB);
            mhDB = 0;
        }
        if (0 != mhDevice) {
            FingerprintSensorEx.CloseDevice(mhDevice);
            mhDevice = 0;
        }
        FingerprintSensorEx.Terminate();
    }

    private void OnCaptureOK(byte[] imgBuf) {
        try {
            writeBitmap(imgBuf, fpWidth, fpHeight, "fingerprint.bmp");
            btnImg.setIcon(new ImageIcon(ImageIO.read(new File("fingerprint.bmp"))));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void OnExtractOK(byte[] template) {
        if (bRegister) {

            String userId = idField.getText();
            if (userId.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Please Input User ID!",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            } else if (!StringUtil.isNumeric(userId)) {
                JOptionPane.showMessageDialog(this,
                        "User ID must be a number!",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            } else {
                if (DB.idExists(userId)) {
                    JOptionPane.showMessageDialog(this,
                            "User ID already exist!",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            int uid = Integer.parseInt(userId);

            int[] fid = new int[1];
            int[] score = new int[1];
            int ret = FingerprintSensorEx.DBIdentify(mhDB, template, fid, score);
            if (ret == 0) {
                textArea.setText("the finger already enroll by " + fid[0] + ", cancel enroll");
                enroll_idx = 0;
                return;
            }
            if (enroll_idx > 0 && FingerprintSensorEx.DBMatch(mhDB, regTempArray[enroll_idx - 1], template) <= 0) {
                textArea.setText("please press the same finger 3 times for the enrollment");
                return;
            }
            System.arraycopy(template, 0, regTempArray[enroll_idx], 0, 2048);
            enroll_idx++;
            if (enroll_idx == 3) {
                int[] _retLen = new int[1];
                _retLen[0] = 2048;
                byte[] regTemp = new byte[_retLen[0]];

                if (0 == (ret = FingerprintSensorEx.DBMerge(mhDB, regTempArray[0], regTempArray[1], regTempArray[2], regTemp, _retLen)) &&
                        0 == (ret = FingerprintSensorEx.DBAdd(mhDB, uid, regTemp))) {
                    cbRegTemp = _retLen[0];
                    System.arraycopy(regTemp, 0, lastRegTemp, 0, cbRegTemp);

                    // store only success
                    System.out.println("insert data to db");
                    String base64 = FingerprintSensorEx.BlobToBase64(lastRegTemp, cbRegTemp);
                    SubConstructor subConstructor = new SubConstructor()
                            .setId(uid)
                            .setBase64(base64);
                    if (ServerSyncService.sentToRemoteServer(subConstructor)) {
                        if (DB.insert(subConstructor)) {
                            JOptionPane.showMessageDialog(this,
                                    String.format("User with ID %s registered success fully", uid),
                                    "Success",
                                    JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                    idField.setText(uid + 1 + "");

                } else {
                    textArea.setText("enroll fail, error code = " + ret);
                }
            } else {
                textArea.setText("You need to press the " + (3 - enroll_idx) + " times fingerprint");
            }
        } else if (bIdentify) {
            int[] fid = new int[1];
            int[] score = new int[1];
            int ret = FingerprintSensorEx.DBIdentify(mhDB, template, fid, score);
            if (ret == 0) {
                textArea.setText("Identify success, fid = " + fid[0] + ",score=" + score[0]);
            } else {
                textArea.setText("Identify fail, error code = " + ret);
            }
        }
    }

    private class WorkThread extends Thread {
        @Override
        public void run() {
            super.run();
            int ret;
            while (!mbStop) {
                templateLen[0] = 2048;
                if (0 == FingerprintSensorEx.AcquireFingerprint(mhDevice, imgBuf, template, templateLen)) {
                    if (nFakeFunOn == 1) {
                        byte[] paramValue = new byte[4];
                        int[] size = new int[1];
                        size[0] = 4;
                        int nFakeStatus;
                        //GetFakeStatus
                        ret = FingerprintSensorEx.GetParameters(mhDevice, 2004, paramValue, size);
                        nFakeStatus = byteArrayToInt(paramValue);
                        System.out.println("ret = " + ret + ", nFakeStatus = " + nFakeStatus);
                        if (0 == ret && (byte) (nFakeStatus & 31) != 31) {
                            textArea.setText("Is a fake-finer?");
                            return;
                        }
                    }
                    OnExtractOK(template);
                    OnCaptureOK(imgBuf);
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

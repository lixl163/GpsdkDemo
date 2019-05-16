package com.gpsdk.demo.gpsdkdemo;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.gprinter.command.CpclCommand;
import com.gprinter.command.EscCommand;
import com.gprinter.command.FactoryCommand;
import com.gprinter.command.LabelCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static android.hardware.usb.UsbManager.ACTION_USB_DEVICE_ATTACHED;
import static android.hardware.usb.UsbManager.ACTION_USB_DEVICE_DETACHED;
import static com.gpsdk.demo.gpsdkdemo.Constant.ACTION_USB_PERMISSION;
import static com.gpsdk.demo.gpsdkdemo.Constant.MESSAGE_UPDATE_PARAMETER;
import static com.gpsdk.demo.gpsdkdemo.DeviceConnFactoryManager.ACTION_QUERY_PRINTER_STATE;
import static com.gpsdk.demo.gpsdkdemo.DeviceConnFactoryManager.CONN_STATE_FAILED;


/**
 * Created by Administrator
 *
 * @author 猿史森林
 *         Date: 2017/8/2
 *         Class description:
 */
public class MainActivity extends AppCompatActivity {
    private static final String	TAG	= "MainActivity";
    ArrayList<String>		per	= new ArrayList<>();
    private UsbManager		usbManager;
    private int			counts;
    private static final int	REQUEST_CODE = 0x004;


    /**
     * 连接状态断开
     */
    private static final int CONN_STATE_DISCONN = 0x007;


    /**
     * 使用打印机指令错误
     */
    private static final int PRINTER_COMMAND_ERROR = 0x008;


    /**
     * ESC查询打印机实时状态指令
     */
    private byte[] esc = { 0x10, 0x04, 0x02 };


    /**
     * CPCL查询打印机实时状态指令
     */
    private byte[] cpcl = { 0x1b, 0x68 };


    /**
     * TSC查询打印机状态指令
     */
    private byte[] tsc = { 0x1b, '!', '?' };

    private static final int	CONN_MOST_DEVICES	= 0x11;
    private static final int	CONN_PRINTER		= 0x12;
    private PendingIntent		mPermissionIntent;
    private				String[] permissions = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH
    };
    private String			usbName;
    private TextView		tvConnState;
    private ThreadPool		threadPool;


    /**
     * 判断打印机所使用指令是否是ESC指令
     */
    private int		id = 0;
    private EditText	etPrintCounts;
    private Spinner		mode_sp;
//    private byte[]		tscmode		= { 0x1f, 0x1b, 0x1f, (byte) 0xfc, 0x01, 0x02, 0x03, 0x33 };
//    private byte[]		cpclmode	= { 0x1f, 0x1b, 0x1f, (byte) 0xfc, 0x01, 0x02, 0x03, 0x44 };
//    private byte[]		escmode		= { 0x1f, 0x1b, 0x1f, (byte) 0xfc, 0x01, 0x02, 0x03, 0x55 };
//    private byte[]		selftest	= { 0x1f, 0x1b, 0x1f, (byte) 0x93, 0x10, 0x11, 0x12, 0x15, 0x16, 0x17, 0x10, 0x00 };
    private int		printcount	= 0;
    private boolean		continuityprint = false;
    /* private KeepConn keepConn; */
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        Log.e( TAG, "onCreate()" );
        setContentView( R.layout.activity_main );
        usbManager = (UsbManager) getSystemService( Context.USB_SERVICE );
        checkPermission();
        requestPermission();
        tvConnState	= (TextView) findViewById( R.id.tv_connState );
        etPrintCounts	= (EditText) findViewById( R.id.et_print_counts );
        initsp();
    }


    private void initsp()
    {
        List<String> list = new ArrayList<String>();
        list.add( getString( R.string.str_escmode ) );
        list.add( getString( R.string.str_tscmode ) );
        list.add( getString( R.string.str_cpclmode ) );
        ArrayAdapter<String> adapter = new ArrayAdapter<String>( this,
                android.R.layout.simple_spinner_item, list );
        adapter.setDropDownViewResource( android.R.layout.simple_list_item_single_choice );
        mode_sp = (Spinner) findViewById( R.id.mode_sp );
        mode_sp.setAdapter( adapter );
    }


    @Override
    protected void onStart()
    {
        super.onStart();
        IntentFilter filter = new IntentFilter( ACTION_USB_PERMISSION );
        filter.addAction( ACTION_USB_DEVICE_DETACHED );
        filter.addAction( ACTION_QUERY_PRINTER_STATE );
        filter.addAction( DeviceConnFactoryManager.ACTION_CONN_STATE );
        filter.addAction( ACTION_USB_DEVICE_ATTACHED );
        registerReceiver( receiver, filter );
    }


    private void checkPermission()
    {
        for ( String permission : permissions )
        {
            if ( PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission( this, permission ) )
            {
                per.add( permission );
            }
        }
    }


    private void requestPermission()
    {
        if ( per.size() > 0 )
        {
            String[] p = new String[per.size()];
            ActivityCompat.requestPermissions( this, per.toArray( p ), REQUEST_CODE );
        }
    }


    /**
     * 蓝牙连接
     */
    public void btnBluetoothConn( View view )
    {
        startActivityForResult( new Intent( this, BluetoothDeviceList.class ), Constant.BLUETOOTH_REQUEST_CODE );
    }


    /**
     * 连接多设备
     *
     * @param view
     */
    public void btnMoreDevices( View view )
    {
        startActivityForResult( new Intent( this, ConnMoreDevicesActivity.class ), CONN_MOST_DEVICES );
    }


    /**
     * 串口连接
     *
     * @param view
     */
    public void btnSerialPortConn( View view )
    {
        startActivityForResult( new Intent( this, SerialPortList.class ), Constant.SERIALPORT_REQUEST_CODE );
    }


    /**
     * USB连接
     *
     * @param view
     */
    public void btnUsbConn( View view )
    {
        startActivityForResult( new Intent( this, UsbDeviceList.class ), Constant.USB_REQUEST_CODE );
    }


    /**
     * WIFI连接
     * @param view
     */
    public void btnWifiConn( View view )
    {
        WifiParameterConfigDialog wifiParameterConfigDialog = new WifiParameterConfigDialog( this, mHandler );
        wifiParameterConfigDialog.show();
    }


    /**
     * 测试打印  ----lixl
     * @param view
     */
    public void btnTest( View view )
    {
        if ( DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] == null ||
                !DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getConnState() )
        {
            Utils.toast( this, getString( R.string.str_cann_printer ) );
            return;
        }
        threadPool = ThreadPool.getInstantiation();
        threadPool.addTask( new Runnable()
        {
            @Override
            public void run()
            {
                if ( DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getCurrentPrinterCommand() == PrinterCommand.TSC )
                {
                    sendTest();
                } else {
                    mHandler.obtainMessage( PRINTER_COMMAND_ERROR ).sendToTarget();
                }
            }
        } );
    }

    /**
     * 打印票据
     * @param view
     */
    public void btnReceiptPrint( View view )
    {
        if ( DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] == null ||
                !DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getConnState() )
        {
            Utils.toast( this, getString( R.string.str_cann_printer ) );
            return;
        }
        threadPool = ThreadPool.getInstantiation();
        threadPool.addTask( new Runnable()
        {
            @Override
            public void run()
            {
                if ( DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getCurrentPrinterCommand() == PrinterCommand.ESC )
                {
                    sendReceiptWithResponse();
                } else {
                    mHandler.obtainMessage( PRINTER_COMMAND_ERROR ).sendToTarget();
                }
            }
        } );
    }
    /**
     * 打印面单
     * @param view
     */
    public void btnCpclPrint( View view )
    {
        if ( DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] == null ||
                !DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getConnState() )
        {
            Utils.toast( this, getString( R.string.str_cann_printer ) );
            return;
        }
        threadPool = ThreadPool.getInstantiation();
        threadPool.addTask( new Runnable()
        {
            @Override
            public void run()
            {
                if ( DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getCurrentPrinterCommand() == PrinterCommand.CPCL )
                {
                    sendCpcl();
                } else {
                    mHandler.obtainMessage( PRINTER_COMMAND_ERROR ).sendToTarget();
                }
            }
        } );
    }


    private void sendCpcl()
    {
        CpclCommand cpcl = new CpclCommand();
        cpcl.addInitializePrinter( 1130, 1 );
        cpcl.addJustification( CpclCommand.ALIGNMENT.CENTER );
        cpcl.addSetmag( 1, 1 );
        cpcl.addText( CpclCommand.TEXT_FONT.FONT_4, 0, 30, "Sample" );
        cpcl.addSetmag( 0, 0 );
        cpcl.addJustification( CpclCommand.ALIGNMENT.LEFT );
        cpcl.addText( CpclCommand.TEXT_FONT.FONT_4, 0, 65, "Print text" );
        cpcl.addText( CpclCommand.TEXT_FONT.FONT_4, 0, 95, "Welcom to use SMARNET printer!" );
        cpcl.addText( CpclCommand.TEXT_FONT.FONT_13, 0, 135, "佳博智匯標籤打印機" );
        cpcl.addText( CpclCommand.TEXT_FONT.FONT_4, 0, 195, "智汇" );
        cpcl.addJustification( CpclCommand.ALIGNMENT.CENTER );
        cpcl.addText( CpclCommand.TEXT_FONT.FONT_4, 0, 195, "网络" );
        cpcl.addJustification( CpclCommand.ALIGNMENT.RIGHT );
        cpcl.addText( CpclCommand.TEXT_FONT.FONT_4, 0, 195, "设备" );
        cpcl.addJustification( CpclCommand.ALIGNMENT.LEFT );
        cpcl.addText( CpclCommand.TEXT_FONT.FONT_4, 0, 230, "Print bitmap!" );
        Bitmap bitmap = BitmapFactory.decodeResource( getResources(), R.drawable.gprinter );
        cpcl.addEGraphics( 0, 255, 385, bitmap );
        cpcl.addText( CpclCommand.TEXT_FONT.FONT_4, 0, 645, "Print code128!" );
        cpcl.addBarcodeText( 5, 2 );
        cpcl.addBarcode( CpclCommand.COMMAND.BARCODE, CpclCommand.CPCLBARCODETYPE.CODE128, 50, 0, 680, "SMARNET" );
        cpcl.addText( CpclCommand.TEXT_FONT.FONT_4, 0, 775, "Print QRcode" );
        cpcl.addBQrcode( 0, 810, "QRcode" );
        cpcl.addJustification( CpclCommand.ALIGNMENT.CENTER );
        cpcl.addText( CpclCommand.TEXT_FONT.FONT_4, 0, 1010, "Completed" );
        cpcl.addJustification( CpclCommand.ALIGNMENT.LEFT );
        cpcl.addPrint();
        Vector<Byte> datas = cpcl.getCommand();
        /* 发送数据 */
        DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately( datas );
    }


    private void sendCpcl( int id )
    {
        CpclCommand cpcl = new CpclCommand();
        cpcl.addInitializePrinter( 1130, 1 );
        cpcl.addJustification( CpclCommand.ALIGNMENT.CENTER );
        cpcl.addSetmag( 1, 1 );
        cpcl.addText( CpclCommand.TEXT_FONT.FONT_4, 0, 30, "Sample" );
        cpcl.addSetmag( 0, 0 );
        cpcl.addJustification( CpclCommand.ALIGNMENT.LEFT );
        cpcl.addText( CpclCommand.TEXT_FONT.FONT_4, 0, 65, "Print text" );
        cpcl.addText( CpclCommand.TEXT_FONT.FONT_4, 0, 95, "Welcom to use SMARNET printer!" );
        cpcl.addText( CpclCommand.TEXT_FONT.FONT_13, 0, 135, "佳博智匯標籤打印機" );
        cpcl.addText( CpclCommand.TEXT_FONT.FONT_4, 0, 195, "智汇" );
        cpcl.addJustification( CpclCommand.ALIGNMENT.CENTER );
        cpcl.addText( CpclCommand.TEXT_FONT.FONT_4, 0, 195, "网络" );
        cpcl.addJustification( CpclCommand.ALIGNMENT.RIGHT );
        cpcl.addText( CpclCommand.TEXT_FONT.FONT_4, 0, 195, "设备" );
        cpcl.addJustification( CpclCommand.ALIGNMENT.LEFT );
        cpcl.addText( CpclCommand.TEXT_FONT.FONT_4, 0, 230, "Print bitmap!" );
        Bitmap bitmap = BitmapFactory.decodeResource( getResources(), R.drawable.gprinter );
        cpcl.addEGraphics( 0, 255, 385, bitmap );
        cpcl.addText( CpclCommand.TEXT_FONT.FONT_4, 0, 645, "Print code128!" );
        cpcl.addBarcodeText( 5, 2 );
        cpcl.addBarcode( CpclCommand.COMMAND.BARCODE, CpclCommand.CPCLBARCODETYPE.CODE128, 50, 0, 680, "SMARNET" );
        cpcl.addText( CpclCommand.TEXT_FONT.FONT_4, 0, 775, "Print QRcode" );
        cpcl.addBQrcode( 0, 810, "QRcode" );
        cpcl.addJustification( CpclCommand.ALIGNMENT.CENTER );
        cpcl.addText( CpclCommand.TEXT_FONT.FONT_4, 0, 1010, "Completed" );
        cpcl.addJustification( CpclCommand.ALIGNMENT.LEFT );
        cpcl.addPrint();
        Vector<Byte> datas = cpcl.getCommand();
        /* 发送数据 */
        DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately( datas );
    }


    /**
     * 打印标签
     * @param view
     */
    public void btnLabelPrint( View view )
    {
        threadPool = ThreadPool.getInstantiation();
        threadPool.addTask( new Runnable()
        {
            @Override
            public void run()
            {
                if ( DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] == null ||
                        !DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getConnState() )
                {
                    mHandler.obtainMessage( CONN_PRINTER ).sendToTarget();
                    return;
                }
                if ( DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getCurrentPrinterCommand() == PrinterCommand.TSC )
                {
                    sendLabel();
                } else {
                    mHandler.obtainMessage( PRINTER_COMMAND_ERROR ).sendToTarget();
                }
            }
        } );
    }


    /**
     * 断开连接
     * @param view
     */
    public void btnDisConn( View view )
    {
        if ( DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] == null ||
                !DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getConnState() )
        {
            Utils.toast( this, getString( R.string.str_cann_printer ) );
            return;
        }
        mHandler.obtainMessage( CONN_STATE_DISCONN ).sendToTarget();
    }


    /**
     * 打印自检页
     * @param view
     */
    public void btnPrintSelftest( View view )
    {
        threadPool = ThreadPool.getInstantiation();
        threadPool.addTask( new Runnable()
        {
            @Override
            public void run()
            {
                if ( DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] == null ||
                        !DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getConnState() )
                {
                    mHandler.obtainMessage( CONN_PRINTER ).sendToTarget();
                    return;
                }
//                Vector<Byte> data = new Vector<>( tscmode.length );
//                for ( int i = 0; i < selftest.length; i++ )
//                {
//                    data.add( selftest[i] );
//                }
//                DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately( data );
                byte[] bytes = null;
                if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getCurrentPrinterCommand() == PrinterCommand.TSC){
                    bytes = FactoryCommand.getSelfTest(1);
                }else if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getCurrentPrinterCommand() == PrinterCommand.ESC){
                    bytes = FactoryCommand.getSelfTest(0);
                }
                if (bytes != null){
                    Vector<Byte> data = new Vector<>( bytes.length );
                    for ( int i = 0; i < bytes.length; i++ )
                    {
                        data.add( bytes[i] );
                    }
                    DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately( data );
                }

            }
        } );
    }


    /**
     * 打印XML
     * @param view
     */
    public void btnPrintXml( View view )
    {
        View		v		= View.inflate( this, R.layout.pj, null );
        TableLayout	tableLayout	= (TableLayout) v.findViewById( R.id.li );
        TextView	jine		= (TextView) v.findViewById( R.id.jine );
        TextView	pep		= (TextView) v.findViewById( R.id.pep );
        tableLayout.addView( ctv( MainActivity.this, "红茶\n加热\n加糖", 8, 3 ) );
        tableLayout.addView( ctv( MainActivity.this, "绿茶", 109, 899 ) );
        tableLayout.addView( ctv( MainActivity.this, "咖啡", 15, 4 ) );
        tableLayout.addView( ctv( MainActivity.this, "红茶", 8, 3 ) );
        tableLayout.addView( ctv( MainActivity.this, "绿茶", 10, 8 ) );
        tableLayout.addView( ctv( MainActivity.this, "咖啡", 15, 4 ) );
        tableLayout.addView( ctv( MainActivity.this, "红茶", 8, 3 ) );
        tableLayout.addView( ctv( MainActivity.this, "绿茶", 10, 8 ) );
        tableLayout.addView( ctv( MainActivity.this, "咖啡", 15, 4 ) );
        tableLayout.addView( ctv( MainActivity.this, "红茶", 8, 3 ) );
        tableLayout.addView( ctv( MainActivity.this, "绿茶", 10, 8 ) );
        tableLayout.addView( ctv( MainActivity.this, "咖啡", 15, 4 ) );
        tableLayout.addView( ctv( MainActivity.this, "红茶", 8, 3 ) );
        tableLayout.addView( ctv( MainActivity.this, "绿茶", 10, 8 ) );
        tableLayout.addView( ctv( MainActivity.this, "咖啡", 15, 4 ) );
        jine.setText( "998" );
        pep.setText( "张三" );
        final Bitmap bitmap = convertViewToBitmap( v );
        threadPool = ThreadPool.getInstantiation();
        threadPool.addTask( new Runnable()
        {
            @Override
            public void run()
            {
                if ( DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] == null ||
                        !DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getConnState() )
                {
                    mHandler.obtainMessage( CONN_PRINTER ).sendToTarget();
                    return;
                }

                if ( DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getCurrentPrinterCommand() == PrinterCommand.CPCL )
                {
                    CpclCommand cpcl = new CpclCommand();
                    cpcl.addInitializePrinter( 1500, 1 );
                    cpcl.addCGraphics( 0, 0, (80 - 10) * 8, bitmap );
                    cpcl.addPrint();
                    DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately( cpcl.getCommand() );
                } else if ( DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getCurrentPrinterCommand() == PrinterCommand.TSC )
                {
                    LabelCommand labelCommand = new LabelCommand();
                    labelCommand.addSize( 80, 180 );
                    labelCommand.addCls();
                    labelCommand.addBitmap( 0, 0, (80 - 10) * 8, bitmap );
                    labelCommand.addPrint( 1 );
                    DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately( labelCommand.getCommand() );
                }else if ( DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getCurrentPrinterCommand() == PrinterCommand.ESC )
                {
                    EscCommand esc = new EscCommand();
                    esc.addInitializePrinter();
                    esc.addInitializePrinter();
                    esc.addRastBitImage( bitmap, (80 - 10) * 8, 0 );
                    esc.addPrintAndLineFeed();
                    esc.addPrintAndLineFeed();
                    esc.addPrintAndLineFeed();
                    DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately( esc.getCommand() );
                }
            }
        } );
    }


    /**
     * mxl转bitmap图片
     * @param view
     * @return
     */
    public static Bitmap convertViewToBitmap( View view )
    {
        view.measure( View.MeasureSpec.makeMeasureSpec( 0, View.MeasureSpec.UNSPECIFIED ), View.MeasureSpec.makeMeasureSpec( 0, View.MeasureSpec.UNSPECIFIED ) );
        view.layout( 0, 0, view.getMeasuredWidth(), view.getMeasuredHeight() );
        view.buildDrawingCache();
        Bitmap bitmap = view.getDrawingCache();
        return(bitmap);
    }


    private TableRow ctv( Context context, String name, int k, int n )
    {
        TableRow tb = new TableRow( context );
        tb.setLayoutParams( new TableLayout.LayoutParams( TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT ) );
        TextView tv1 = new TextView( context );
        tv1.setLayoutParams( new TableRow.LayoutParams( TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT ) );
        tv1.setText( name );
        tv1.setTextColor( Color.BLACK );
        tv1.setTextSize( 30 );
        tb.addView( tv1 );
        TextView tv2 = new TextView( context );
        tv2.setLayoutParams( new TableRow.LayoutParams( TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT ) );
        tv2.setText( k + "" );
        tv2.setTextColor( Color.BLACK );
        tv2.setTextSize( 30 );
        tb.addView( tv2 );
        TextView tv3 = new TextView( context );
        tv3.setLayoutParams( new TableRow.LayoutParams( TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT ) );
        tv3.setText( n + "" );
        tv3.setTextColor( Color.BLACK );
        tv3.setTextSize( 30 );
        tb.addView( tv3 );
        return(tb);
    }


    /**
     * 打印机状态查询
     *
     * @param view
     */
    public void btnPrinterState( View view )
    {
        /* 打印机状态查询 */
        if ( DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] == null ||
                !DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getConnState() )
        {
            Utils.toast( this, getString( R.string.str_cann_printer ) );
            return;
        }
        DeviceConnFactoryManager.whichFlag = true;
        ThreadPool.getInstantiation().addTask( new Runnable()
        {
            @Override
            public void run()
            {
                Vector<Byte> data = new Vector<>( esc.length );
                if ( DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getCurrentPrinterCommand() == PrinterCommand.ESC )
                {
                    for ( int i = 0; i < esc.length; i++ )
                    {
                        data.add( esc[i] );
                    }
                    DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately( data );
                }else if ( DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getCurrentPrinterCommand() == PrinterCommand.TSC )
                {
                    for ( int i = 0; i < tsc.length; i++ )
                    {
                        data.add( tsc[i] );
                    }
                    DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately( data );
                }else if ( DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getCurrentPrinterCommand() == PrinterCommand.CPCL )
                {
                    for ( int i = 0; i < cpcl.length; i++ )
                    {
                        data.add( cpcl[i] );
                    }
                    DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately( data );
                }
            }
        } );
    }

    /**
     * 电量查询(仅票据可查询)
     * @param v
     */

    public void btnPrinterPower(View v){
        /* 打印机状态查询 */
        if ( DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] == null ||
                !DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getConnState() )
        {
            Utils.toast( this, getString( R.string.str_cann_printer ) );
            return;
        }
        DeviceConnFactoryManager.whichFlag = false;
        ThreadPool.getInstantiation().addTask( new Runnable()
        {
            @Override
            public void run()
            {
                if ( DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getCurrentPrinterCommand() == PrinterCommand.ESC ) {
                    byte[] bytes = FactoryCommand.searchPower(0);
                    Vector<Byte> data = new Vector<>(bytes.length);
                    for (int i = 0; i < bytes.length; i++) {
                        data.add(bytes[i]);
                    }
                    DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately( data );
                }
            }
        } );
    }



    /**
     * 多设备同步打印
     */
    public void btnSynchronousPrint( View view )
    {
        int device = 0;
        for ( int i = 0; i < 4; i++ )
        {
            final int id = i;
            if ( DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] == null ||
                    !DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getConnState() )
            {
                device++;
                if ( device == 4 )
                {
                    Utils.toast( this, getString( R.string.str_cann_printer ) );
                }
                continue;
            }
            threadPool = ThreadPool.getInstantiation();
            threadPool.addTask( new Runnable()
            {
                @Override
                public void run()
                {
                    if ( DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getCurrentPrinterCommand() == PrinterCommand.CPCL )
                    {
                        sendCpcl( id );
                    } else if ( DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getCurrentPrinterCommand() == PrinterCommand.TSC )
                    {
                        sendLabel( id );
                    }else if ( DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getCurrentPrinterCommand() == PrinterCommand.ESC )
                    {
                        sendReceiptWithResponse( id );
                    } else {
                        mHandler.obtainMessage( PRINTER_COMMAND_ERROR ).sendToTarget();
                    }
                }
            } );
        }
    }


    /**
     * 更换打印模式
     * @param view
     */
    public void btnModeChange( View view )
    {
        if ( DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] == null ||
                !DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getConnState() )
        {
            Utils.toast( this, getString( R.string.str_cann_printer ) );
            return;
        }
        if ( DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getCurrentPrinterCommand() == PrinterCommand.CPCL )
        {
            CpclCommand cpclCommand = new CpclCommand();
            cpclCommand.addInitializePrinter();
            DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately( cpclCommand.getCommand() );
        }
        int sp_no = mode_sp.getSelectedItemPosition(); //0票据,1标签,2面单
        byte[] bytes = FactoryCommand.changeWorkMode(sp_no);
        Vector<Byte> data = new Vector<>();
        for (int i = 0; i < bytes.length; i++) {
            data.add(bytes[i]);
        }
        DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately( data );
        DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].closePort( id );
    }


    /**
     * 连续打印
     *
     * @param view
     */
    public void btnReceiptAndLabelContinuityPrint( View view )
    {
        if ( DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] == null ||
                !DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getConnState() )
        {
            Utils.toast( this, getString( R.string.str_cann_printer ) );
            return;
        }
        if ( etPrintCounts.getText().toString().trim().isEmpty() )
        {
            Utils.toast( this, getString( R.string.str_continuity_count ) );
            return;
        }
        counts		= Integer.parseInt( etPrintCounts.getText().toString().trim() );
        printcount	= 0;
        continuityprint = true;
        sendContinuityPrint();
    }


    /*
     *        int sss(int i){   //判断打印第几个商品
     *            int z=0;
     *            for (int j=0;j<i+1;j++){
     *                z+=sl[j];
     *            }
     *            return counts+z;
     *        }
     */
    private void sendContinuityPrint()
    {
        ThreadPool.getInstantiation().addTask( new Runnable()
        {
            @Override
            public void run()
            {
                if ( DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] != null
                        && DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getConnState() )
                {
                    ThreadFactoryBuilder threadFactoryBuilder = new ThreadFactoryBuilder( "MainActivity_sendContinuity_Timer" );
                    ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor( 1, threadFactoryBuilder );
                    scheduledExecutorService.schedule( threadFactoryBuilder.newThread( new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            counts--;
                            if ( DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getCurrentPrinterCommand() == PrinterCommand.ESC )
                            {
                                sendReceiptWithResponse();
                            } else if ( DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getCurrentPrinterCommand() == PrinterCommand.TSC )
                            {
                                /* 标签模式可直接使用LabelCommand.addPrint()方法进行打印 */
                                sendLabel();
                                /*
                                 *                                for(int i=0;i<sl.length;i++){ //  8个商品，8个数量
                                 *                                    if(sss(i)>=36){
                                 *                                        sendLabel(i,"第"+i+"个商品");
                                 *                                        break;
                                 *                                    }
                                 *                                }
                                 */
                            }else {
                                sendCpcl();
                            }
                        }
                    } ), 1000, TimeUnit.MILLISECONDS );
                }
            }
        } );
    }


    @Override
    protected void onActivityResult( int requestCode, int resultCode, Intent data )
    {
        super.onActivityResult( requestCode, resultCode, data );
        if ( resultCode == RESULT_OK )
        {
            switch ( requestCode )
            {
                /*蓝牙连接*/
                case Constant.BLUETOOTH_REQUEST_CODE: {
                    closeport();
                    /*获取蓝牙mac地址*/
                    String macAddress = data.getStringExtra( BluetoothDeviceList.EXTRA_DEVICE_ADDRESS );
                    /* 初始化话DeviceConnFactoryManager */
                    new DeviceConnFactoryManager.Build()
                            .setId( id )
                            /* 设置连接方式 */
                            .setConnMethod( DeviceConnFactoryManager.CONN_METHOD.BLUETOOTH )
                            /* 设置连接的蓝牙mac地址 */
                            .setMacAddress( macAddress )
                            .build();
                    /* 打开端口 */
                    Log.d(TAG, "onActivityResult: 连接蓝牙"+id);
                    threadPool = ThreadPool.getInstantiation();
                    threadPool.addTask( new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].openPort();
                        }
                    } );

                    break;
                }
                /*USB连接*/
                case Constant.USB_REQUEST_CODE: {
                    closeport();
                    /* 获取USB设备名 */
                    usbName = data.getStringExtra( UsbDeviceList.USB_NAME );
                    /* 通过USB设备名找到USB设备 */
                    UsbDevice usbDevice = Utils.getUsbDeviceFromName( MainActivity.this, usbName );
                    /* 判断USB设备是否有权限 */
                    if ( usbManager.hasPermission( usbDevice ) )
                    {
                        usbConn( usbDevice );
                    } else {        /* 请求权限 */
                        mPermissionIntent = PendingIntent.getBroadcast( this, 0, new Intent( ACTION_USB_PERMISSION ), 0 );
                        usbManager.requestPermission( usbDevice, mPermissionIntent );
                    }
                    break;
                }
                /*串口连接*/
                case Constant.SERIALPORT_REQUEST_CODE:
                    closeport();
                    /* 获取波特率 */
                    int baudrate = data.getIntExtra( Constant.SERIALPORTBAUDRATE, 0 );
                    /* 获取串口号 */
                    String path = data.getStringExtra( Constant.SERIALPORTPATH );

                    if ( baudrate != 0 && !TextUtils.isEmpty( path ) )
                    {
                        /* 初始化DeviceConnFactoryManager */
                        new DeviceConnFactoryManager.Build()
                                /* 设置连接方式 */
                                .setConnMethod( DeviceConnFactoryManager.CONN_METHOD.SERIAL_PORT )
                                .setId( id )
                                /* 设置波特率 */
                                .setBaudrate( baudrate )
                                /* 设置串口号 */
                                .setSerialPort( path )
                                .build();
                        /* 打开端口 */
                        DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].openPort();
                    }
                    break;
                case CONN_MOST_DEVICES:
                    id = data.getIntExtra( "id", -1 );
                    if ( DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] != null &&
                            DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getConnState() )
                    {
                        tvConnState.setText( getString( R.string.str_conn_state_connected ) + "\n" + getConnDeviceInfo() );
                    } else {
                        tvConnState.setText( getString( R.string.str_conn_state_disconnect ) );
                    }
                    break;
                default:
                    break;
            }
        }
    }


    /**
     * 重新连接回收上次连接的对象，避免内存泄漏
     */
    private void closeport()
    {
        if ( DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] != null &&DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].mPort != null )
        {
            DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].reader.cancel();
            DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].mPort.closePort();
            DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].mPort = null;
        }
    }


    /**
     * usb连接
     *
     * @param usbDevice
     */
    private void usbConn( UsbDevice usbDevice )
    {
        new DeviceConnFactoryManager.Build()
                .setId( id )
                .setConnMethod( DeviceConnFactoryManager.CONN_METHOD.USB )
                .setUsbDevice( usbDevice )
                .setContext( this )
                .build();
        DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].openPort();
    }

    /**
     * 自定义打印内容  ----lixl
     */
    void sendTest(){

        LabelCommand tsc = new LabelCommand();
        /* 设置标签尺寸，按照实际尺寸设置，以mm为单位 */
        tsc.addSize( 40, 40 );

        /* 设置标签间隙，按照实际尺寸设置，如果为无间隙纸则设置为0，以mm为单位 */
        tsc.addGap( 2 );

        /* 设置打印方向 */
        tsc.addDirection( LabelCommand.DIRECTION.FORWARD, LabelCommand.MIRROR.NORMAL );
        /* 设置原点坐标 */
        tsc.addReference( 0, 0 );

        /* 开启带Response的打印，用于连续打印 */
        tsc.addQueryPrinterStatus( LabelCommand.RESPONSE_MODE.ON );

        /* 撕纸模式开启 */
//        tsc.addTear( EscCommand.ENABLE.ON );

        /* 清除打印缓冲区 */
        tsc.addCls();

        /* 绘制简体中文，以英寸为单位39约为1cm，字体以点为单位24大约3mm*/
        tsc.addText( 0, 40, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                "金黄蕉(福门).老挝.箱装" );
        tsc.addText( 0, 40+24+6, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                "果速送2019-04-24 09:39:40" );

        /* 绘制图片,width以点为单位，所有除以8 */
//        Bitmap b = BitmapFactory.decodeResource( getResources(), R.drawable.ercode );
//        tsc.addBitmap( 39+40, 39+24+24+12, LabelCommand.BITMAP_MODE.OVERWRITE, 160, b );

        /* 绘制二维码 */
        tsc.addQRCode( 60, 40+24+6+24+12, LabelCommand.EEC.LEVEL_L, 8, LabelCommand.ROTATION.ROTATION_0, " www.smarnet.cc" );

        /* 打印标签 */
        tsc.addPrint( 1, 1 );

        /* 打印标签后 蜂鸣器响 */
//        tsc.addSound( 2, 100 );

        //产生钱箱控制脉冲
//        tsc.addCashdrwer( LabelCommand.FOOT.F5, 255, 255 );

        Vector<Byte> datas = tsc.getCommand();
        /* 发送数据 */
        if ( DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] == null )
        {
            Log.d(TAG, "sendLabel: 打印机为空");
            return;
        }
        DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately( datas );

    }

    /**
     * 发送标签
     */
    void sendLabel()
    {
        LabelCommand tsc = new LabelCommand();
        /* 设置标签尺寸，按照实际尺寸设置 */
        tsc.addSize( 80, 90 );
        /* 设置标签间隙，按照实际尺寸设置，如果为无间隙纸则设置为0 */
        tsc.addGap( 0 );
        /* 设置打印方向 */
        tsc.addDirection( LabelCommand.DIRECTION.FORWARD, LabelCommand.MIRROR.NORMAL );
        /* 开启带Response的打印，用于连续打印 */
        tsc.addQueryPrinterStatus( LabelCommand.RESPONSE_MODE.ON );
        /* 设置原点坐标 */
        tsc.addReference( 0, 0 );
        /* 撕纸模式开启 */
        tsc.addTear( EscCommand.ENABLE.ON );
        /* 清除打印缓冲区 */
        tsc.addCls();
        /* 绘制简体中文 */
        tsc.addText( 10, 0, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                "Welcome to use SMARNET printer" );
        /* 绘制图片 */
        Bitmap b = BitmapFactory.decodeResource( getResources(), R.drawable.gprinter );
        tsc.addBitmap( 10, 20, LabelCommand.BITMAP_MODE.OVERWRITE, 300, b );

        tsc.addQRCode( 10, 330, LabelCommand.EEC.LEVEL_L, 5, LabelCommand.ROTATION.ROTATION_0, " www.smarnet.cc" );
        /* 绘制一维条码 */
        tsc.add1DBarcode( 10, 450, LabelCommand.BARCODETYPE.CODE128, 100, LabelCommand.READABEL.EANBEL, LabelCommand.ROTATION.ROTATION_0, "SMARNET" );

        tsc.addText(10, 580, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                "简体字" );

        tsc.addText(100, 580, LabelCommand.FONTTYPE.TRADITIONAL_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                "繁體字" );

        tsc.addText(190, 580, LabelCommand.FONTTYPE.KOREAN, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                "한국어" );

        /* 打印标签 */
        tsc.addPrint( 1, 1 );
        /* 打印标签后 蜂鸣器响 */

        tsc.addSound( 2, 100 );
        tsc.addCashdrwer( LabelCommand.FOOT.F5, 255, 255 );
        Vector<Byte> datas = tsc.getCommand();
        /* 发送数据 */
        if ( DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] == null )
        {
            Log.d(TAG, "sendLabel: 打印机为空");
            return;
        }
        DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately( datas );
    }


    /*
     *    String[]naica={"商品1","商品2","商品3","商品4","商品5","商品6","商品7","商品8"};
     *    int [] sl={3,3,4,4,5,5,6,6};
     *    void sendLabel(int i,String name) {
     *        LabelCommand tsc = new LabelCommand();
     *        // 设置标签尺寸，按照实际尺寸设置
     *        tsc.addSize(60, 10);
     *        // 设置标签间隙，按照实际尺寸设置，如果为无间隙纸则设置为0
     *        tsc.addGap(0);
     *        // 设置打印方向
     *        tsc.addDirection(LabelCommand.DIRECTION.BACKWARD, LabelCommand.MIRROR.NORMAL);
     *        // 开启带Response的打印，用于连续打印
     *        tsc.addQueryPrinterStatus(LabelCommand.RESPONSE_MODE.ON);
     *        // 设置原点坐标
     *        tsc.addReference(0, 0);
     *        // 撕纸模式开启
     *        tsc.addTear(EscCommand.ENABLE.ON);
     *        // 清除打印缓冲区
     *        tsc.addCls();
     *        // 绘制简体中文
     *        tsc.addText(10, 0, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_2, LabelCommand.FONTMUL.MUL_2,
     *                naica[i]);
     *        tsc.addPrint(1, 1);
     *        // 打印标签后 蜂鸣器响
     *        tsc.addSound(2, 100);
     *        tsc.addCashdrwer(LabelCommand.FOOT.F5, 255, 255);
     *        Vector<Byte> datas = tsc.getCommand();
     *        // 发送数据
     *        if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] == null) {
     *            return;
     *        }
     *        DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately(datas);
     *    }
     */

    void sendLabel( int id )
    {
        LabelCommand tsc = new LabelCommand();
        /* 设置标签尺寸，按照实际尺寸设置 */
        tsc.addSize( 60, 60 );
        /* 设置标签间隙，按照实际尺寸设置，如果为无间隙纸则设置为0 */
        tsc.addGap( 0 );
        /* 设置打印方向 */
        tsc.addDirection( LabelCommand.DIRECTION.BACKWARD, LabelCommand.MIRROR.NORMAL );
        /* 开启带Response的打印，用于连续打印 */
        tsc.addQueryPrinterStatus( LabelCommand.RESPONSE_MODE.ON );
        /* 设置原点坐标 */
        tsc.addReference( 0, 0 );
        /* 撕纸模式开启 */
        tsc.addTear( EscCommand.ENABLE.ON );
        /* 清除打印缓冲区 */
        tsc.addCls();
        /* 绘制简体中文 */
        tsc.addText( 10, 0, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                "Welcome to use SMARNET printer!" );
        /* 绘制图片 */
        Bitmap b = BitmapFactory.decodeResource( getResources(), R.drawable.gprinter );
        tsc.addBitmap( 10, 20, LabelCommand.BITMAP_MODE.OVERWRITE, 300, b );

        tsc.addQRCode( 250, 80, LabelCommand.EEC.LEVEL_L, 5, LabelCommand.ROTATION.ROTATION_0, " www.smarnet.cc" );
        /* 绘制一维条码 */
        tsc.add1DBarcode( 20, 250, LabelCommand.BARCODETYPE.CODE128, 100, LabelCommand.READABEL.EANBEL, LabelCommand.ROTATION.ROTATION_0, "SMARNET" );
        /* 打印标签 */
        tsc.addPrint( 1, 1 );
        /* 打印标签后 蜂鸣器响 */

        tsc.addSound( 2, 100 );
        tsc.addCashdrwer( LabelCommand.FOOT.F5, 255, 255 );
        Vector<Byte> datas = tsc.getCommand();
        /* 发送数据 */
        if ( DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] == null )
        {
            return;
        }
        DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately( datas );
    }


    /**
     * 发送票据
     */
    void sendReceiptWithResponse()
    {
        EscCommand esc = new EscCommand();
        esc.addInitializePrinter();
        esc.addPrintAndFeedLines( (byte) 3 );
        /* 设置打印居中 */
        esc.addSelectJustification( EscCommand.JUSTIFICATION.CENTER );
        /* 设置为倍高倍宽 */
        esc.addSelectPrintModes( EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.ON, EscCommand.ENABLE.ON, EscCommand.ENABLE.OFF );
        /* 打印文字 */
        esc.addText( "Sample\n" );
        esc.addPrintAndLineFeed();

        /* 打印文字 */
        /* 取消倍高倍宽 */
        esc.addSelectPrintModes( EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF );
        /* 设置打印左对齐 */
        esc.addSelectJustification( EscCommand.JUSTIFICATION.LEFT );
        /* 打印文字 */
        esc.addText( "Print text\n" );
        /* 打印文字 */
        esc.addText( "Welcome to use SMARNET printer!\n" );

        /* 打印繁体中文 需要打印机支持繁体字库 */
        String message = "佳博智匯票據打印機\n";
        esc.addText( message, "GB2312" );
        esc.addPrintAndLineFeed();

        /* 绝对位置 具体详细信息请查看GP58编程手册 */
        esc.addText( "智汇" );
        esc.addSetHorAndVerMotionUnits( (byte) 7, (byte) 0 );
        esc.addSetAbsolutePrintPosition( (short) 6 );
        esc.addText( "网络" );
        esc.addSetAbsolutePrintPosition( (short) 10 );
        esc.addText( "设备" );
        esc.addPrintAndLineFeed();

        /* 打印图片 */
        /* 打印文字 */
        esc.addText( "Print bitmap!\n" );
        Bitmap b = BitmapFactory.decodeResource( getResources(),
                R.drawable.gprinter );
        /* 打印图片 */
        esc.addRastBitImage( b, 380, 0 );

        /* 打印一维条码 */
        /* 打印文字 */
        esc.addText( "Print code128\n" );
        esc.addSelectPrintingPositionForHRICharacters( EscCommand.HRI_POSITION.BELOW );
        /*
         * 设置条码可识别字符位置在条码下方
         * 设置条码高度为60点
         */
        esc.addSetBarcodeHeight( (byte) 60 );
        /* 设置条码单元宽度为1 */
        esc.addSetBarcodeWidth( (byte) 1 );
        /* 打印Code128码 */
        esc.addCODE128( esc.genCodeB( "SMARNET" ) );
        esc.addPrintAndLineFeed();


        /*
         * QRCode命令打印 此命令只在支持QRCode命令打印的机型才能使用。 在不支持二维码指令打印的机型上，则需要发送二维条码图片
         */
        /* 打印文字 */
        esc.addText( "Print QRcode\n" );
        /* 设置纠错等级 */
        esc.addSelectErrorCorrectionLevelForQRCode( (byte) 0x31 );
        /* 设置qrcode模块大小 */
        esc.addSelectSizeOfModuleForQRCode( (byte) 3 );
        /* 设置qrcode内容 */
        esc.addStoreQRCodeData( "www.smarnet.cc" );
        esc.addPrintQRCode(); /* 打印QRCode */
        esc.addPrintAndLineFeed();

        /* 设置打印左对齐 */
        esc.addSelectJustification( EscCommand.JUSTIFICATION.CENTER );
        /* 打印文字 */
        esc.addText( "Completed!\r\n" );

        /* 开钱箱 */
        esc.addGeneratePlus( LabelCommand.FOOT.F5, (byte) 255, (byte) 255 );
        esc.addPrintAndFeedLines( (byte) 8 );
        /* 加入查询打印机状态，用于连续打印 */
        byte[] bytes = { 29, 114, 1 };
        esc.addUserCommand( bytes );
        Vector<Byte> datas = esc.getCommand();
        /* 发送数据 */
        DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately( datas );
    }


    void sendReceiptWithResponse( int id )
    {
        EscCommand esc = new EscCommand();
        esc.addInitializePrinter();
        esc.addPrintAndFeedLines( (byte) 3 );
        /* 设置打印居中 */
        esc.addSelectJustification( EscCommand.JUSTIFICATION.CENTER );
        /* 设置为倍高倍宽 */
        esc.addSelectPrintModes( EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.ON, EscCommand.ENABLE.ON, EscCommand.ENABLE.OFF );
        /* 打印文字 */
        esc.addText( "Sample\n" );
        esc.addPrintAndLineFeed();

        /* 打印文字 */
        /* 取消倍高倍宽 */
        esc.addSelectPrintModes( EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF );
        /* 设置打印左对齐 */
        esc.addSelectJustification( EscCommand.JUSTIFICATION.LEFT );
        /* 打印文字 */
        esc.addText( "Print text\n" );
        /* 打印文字 */
        esc.addText( "Welcome to use SMARNET printer!\n" );

        /* 打印繁体中文 需要打印机支持繁体字库 */
        String message = "佳博智匯票據打印機\n";
        esc.addText( message, "GB2312" );
        esc.addPrintAndLineFeed();

        /* 绝对位置 具体详细信息请查看GP58编程手册 */
        esc.addText( "智汇" );
        esc.addSetHorAndVerMotionUnits( (byte) 7, (byte) 0 );
        esc.addSetAbsolutePrintPosition( (short) 6 );
        esc.addText( "网络" );
        esc.addSetAbsolutePrintPosition( (short) 10 );
        esc.addText( "设备" );
        esc.addPrintAndLineFeed();

        /* 打印图片 */
        /* 打印文字 */
        esc.addText( "Print bitmap!\n" );
        Bitmap b = BitmapFactory.decodeResource( getResources(),
                R.drawable.gprinter );
        /* 打印图片 */
        esc.addRastBitImage( b, 380, 0 );

        /* 打印一维条码 */
        /* 打印文字 */
        esc.addText( "Print code128\n" );
        esc.addSelectPrintingPositionForHRICharacters( EscCommand.HRI_POSITION.BELOW );
        /*
         * 设置条码可识别字符位置在条码下方
         * 设置条码高度为60点
         */
        esc.addSetBarcodeHeight( (byte) 60 );
        /* 设置条码单元宽度为1 */
        esc.addSetBarcodeWidth( (byte) 1 );
        /* 打印Code128码 */
        esc.addCODE128( esc.genCodeB( "SMARNET" ) );
        esc.addPrintAndLineFeed();


        /*
         * QRCode命令打印 此命令只在支持QRCode命令打印的机型才能使用。 在不支持二维码指令打印的机型上，则需要发送二维条码图片
         */
        /* 打印文字 */
        esc.addText( "Print QRcode\n" );
        /* 设置纠错等级 */
        esc.addSelectErrorCorrectionLevelForQRCode( (byte) 0x31 );
        /* 设置qrcode模块大小 */
        esc.addSelectSizeOfModuleForQRCode( (byte) 3 );
        /* 设置qrcode内容 */
        esc.addStoreQRCodeData( "www.smarnet.cc" );
        esc.addPrintQRCode(); /* 打印QRCode */
        esc.addPrintAndLineFeed();

        /* 设置打印左对齐 */
        esc.addSelectJustification( EscCommand.JUSTIFICATION.CENTER );
        /* 打印文字 */
        esc.addText( "Completed!\r\n" );

        /* 开钱箱 */
        esc.addGeneratePlus( LabelCommand.FOOT.F5, (byte) 255, (byte) 255 );
        esc.addPrintAndFeedLines( (byte) 8 );
        /* 加入查询打印机状态，用于连续打印 */
        byte[] bytes = { 29, 114, 1 };
        Vector<Byte> datas = esc.getCommand();
        /* 发送数据 */
        DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately( datas );
    }


    /**
     * 停止连续打印
     */
    public void btnStopContinuityPrint( View v )
    {
        if ( DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] == null ||
                !DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getConnState() )
        {
            Utils.toast( this, getString( R.string.str_cann_printer ) );
            return;
        }
        if ( counts != 0 )
        {
            counts = 0;
            Utils.toast( this, getString( R.string.str_stop_continuityprint_success ) );
        }
    }


    private BroadcastReceiver receiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive( Context context, Intent intent )
        {
            String action = intent.getAction();
            switch ( action )
            {
                case ACTION_USB_PERMISSION:
                    synchronized (this) {
                        UsbDevice device = intent.getParcelableExtra( UsbManager.EXTRA_DEVICE );
                        if ( intent.getBooleanExtra( UsbManager.EXTRA_PERMISSION_GRANTED, false ) )
                        {
                            if ( device != null )
                            {
                                System.out.println( "permission ok for device " + device );
                                usbConn( device );
                            }
                        } else {
                            System.out.println( "permission denied for device " + device );
                        }
                    }
                    break;
                /* Usb连接断开、蓝牙连接断开广播 */
                case ACTION_USB_DEVICE_DETACHED:
                    mHandler.obtainMessage( CONN_STATE_DISCONN ).sendToTarget();
                    break;
                case DeviceConnFactoryManager.ACTION_CONN_STATE:
                    int state = intent.getIntExtra( DeviceConnFactoryManager.STATE, -1 );
                    int deviceId = intent.getIntExtra( DeviceConnFactoryManager.DEVICE_ID, -1 );
                    switch ( state )
                    {
                        case DeviceConnFactoryManager.CONN_STATE_DISCONNECT:
                            if ( id == deviceId )
                            {
                                tvConnState.setText( getString( R.string.str_conn_state_disconnect ) );
                            }
                            break;
                        case DeviceConnFactoryManager.CONN_STATE_CONNECTING:
                            tvConnState.setText( getString( R.string.str_conn_state_connecting ) );
                            break;
                        case DeviceConnFactoryManager.CONN_STATE_CONNECTED:
                            tvConnState.setText( getString( R.string.str_conn_state_connected ) + "\n" + getConnDeviceInfo() );
                            /*
                             *                            if(DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].connMethod== DeviceConnFactoryManager.CONN_METHOD.WIFI){
                             *                                wificonn=true;
                             *                                if(keepConn==null) {
                             *                                    keepConn = new KeepConn();
                             *                                    keepConn.start();
                             *                                }
                             *                            }
                             */
                            break;
                        case CONN_STATE_FAILED:
                            Utils.toast( MainActivity.this, getString( R.string.str_conn_fail ) );
                            /* wificonn=false; */
                            tvConnState.setText( getString( R.string.str_conn_state_disconnect ) );
                            break;
                        default:
                            break;
                    }
                    break;
                case ACTION_QUERY_PRINTER_STATE:
                    if ( counts >= 0 )
                    {
                        if ( continuityprint )
                        {
                            printcount++;
                            Utils.toast( MainActivity.this, getString( R.string.str_continuityprinter ) + " " + printcount );
                        }
                        if ( counts != 0 )
                        {
                            sendContinuityPrint();
                        }else {
                            continuityprint = false;
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    };
    private Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage( Message msg )
        {
            switch ( msg.what )
            {
                case CONN_STATE_DISCONN:
                    if ( DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] != null || !DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getConnState() )
                    {
                        DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].closePort( id );
                        Utils.toast( MainActivity.this, getString( R.string.str_disconnect_success ) );
                    }
                    break;
                case PRINTER_COMMAND_ERROR:
                    Utils.toast( MainActivity.this, getString( R.string.str_choice_printer_command ) );
                    break;
                case CONN_PRINTER:
                    Utils.toast( MainActivity.this, getString( R.string.str_cann_printer ) );
                    break;
                case MESSAGE_UPDATE_PARAMETER:
                    String strIp = msg.getData().getString( "Ip" );
                    String strPort = msg.getData().getString( "Port" );
                    /* 初始化端口信息 */
                    new DeviceConnFactoryManager.Build()
                            /* 设置端口连接方式 */
                            .setConnMethod( DeviceConnFactoryManager.CONN_METHOD.WIFI )
                            /* 设置端口IP地址 */
                            .setIp( strIp )
                            /* 设置端口ID（主要用于连接多设备） */
                            .setId( id )
                            /* 设置连接的热点端口号 */
                            .setPort( Integer.parseInt( strPort ) )
                            .build();
                    threadPool = ThreadPool.getInstantiation();
                    threadPool.addTask( new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].openPort();
                        }
                    } );
                    break;
                default:
                    new DeviceConnFactoryManager.Build()
                            /* 设置端口连接方式 */
                            .setConnMethod( DeviceConnFactoryManager.CONN_METHOD.WIFI )
                            /* 设置端口IP地址 */
                            .setIp( "192.168.2.227" )
                            /* 设置端口ID（主要用于连接多设备） */
                            .setId( id )
                            /* 设置连接的热点端口号 */
                            .setPort( 9100 )
                            .build();
                    threadPool.addTask( new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].openPort();
                        }
                    } );
                    break;
            }
        }
    };

    @Override
    protected void onStop()
    {
        super.onStop();
        unregisterReceiver( receiver );
    }


    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        Log.e( TAG, "onDestroy()" );
        DeviceConnFactoryManager.closeAllPort();
        if ( threadPool != null )
        {
            threadPool.stopThreadPool();
            threadPool = null;
        }
    }


    private String getConnDeviceInfo()
    {
        String				str				= "";
        DeviceConnFactoryManager	deviceConnFactoryManager	= DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id];
        if ( deviceConnFactoryManager != null
                && deviceConnFactoryManager.getConnState() )
        {
            if ( "USB".equals( deviceConnFactoryManager.getConnMethod().toString() ) )
            {
                str	+= "USB\n";
                str	+= "USB Name: " + deviceConnFactoryManager.usbDevice().getDeviceName();
            } else if ( "WIFI".equals( deviceConnFactoryManager.getConnMethod().toString() ) )
            {
                str	+= "WIFI\n";
                str	+= "IP: " + deviceConnFactoryManager.getIp() + "\t";
                str	+= "Port: " + deviceConnFactoryManager.getPort();
            } else if ( "BLUETOOTH".equals( deviceConnFactoryManager.getConnMethod().toString() ) )
            {
                str	+= "BLUETOOTH\n";
                str	+= "MacAddress: " + deviceConnFactoryManager.getMacAddress();
            } else if ( "SERIAL_PORT".equals( deviceConnFactoryManager.getConnMethod().toString() ) )
            {
                str	+= "SERIAL_PORT\n";
                str	+= "Path: " + deviceConnFactoryManager.getSerialPortPath() + "\t";
                str	+= "Baudrate: " + deviceConnFactoryManager.getBaudrate();
            }
        }
        return(str);
    }


    /*
     *    private boolean wificonn=false;
     *
     *    public String Ping(String str) {
     *        String resault = "";
     *        Process p;
     *        try {
     *            p = Runtime.getRuntime().exec("ping -c 1 -w 3 " + str);
     *            InputStream input = p.getInputStream();
     *            BufferedReader in = new BufferedReader(new InputStreamReader(input));
     *            StringBuffer buffer = new StringBuffer();
     *            String line = "";
     *            while ((line = in.readLine()) != null){
     *                buffer.append(line);
     *            }
     *            System.out.println("Return ============" + buffer.toString());
     *            if(buffer.toString().indexOf("100%")!=-1){
     *                resault = "";
     *            }  else{
     *                resault = "success";
     *            }
     *        } catch (IOException e) {
     *            e.printStackTrace();
     *        }
     *        return resault;
     *    }
     *
     *    class KeepConn extends Thread{
     *        private boolean runing=false;
     *        public KeepConn(){
     *            runing=true;
     *        }
     *        @Override
     *        public void run() {
     *            while (runing){
     *                try {
     *                    sleep(2*1000);
     *                } catch (InterruptedException e) {
     *                    e.printStackTrace();
     *                }
     *                byte[] bytes={0x10,0x04,0x02};
     *                try {
     *                    String str=Ping("192.168.2.227");
     *
     *                    if(str.equals("success")) {
     *                        Log.v("11111111111ss111111111", "Ping结果=======================================success");
     *                       if(wificonn) {
     *                           DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].mPort.getOutputStream().write(bytes);
     *                       }else {
     *                           mHandler.obtainMessage(9999).sendToTarget();
     *                           sleep(10 * 1000);
     *                       }
     *                    }else {
     *                        Log.v("11111111111ss111111111", "Ping结果=======================================fail");
     *                        if(wificonn){
     *                            wificonn=false;
     *                            mHandler.obtainMessage(CONN_STATE_DISCONN).sendToTarget();
     *                        }
     *                    }
     *                } catch (IOException e) {
     *                    e.printStackTrace();
     *                } catch (InterruptedException e) {
     *                    e.printStackTrace();
     *                }
     *            }
     *        }
     *        public void cancel(){
     *            runing=false;
     *        }
     *    }
     */
}
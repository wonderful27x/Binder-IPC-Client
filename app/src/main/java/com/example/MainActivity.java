package com.example;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.binder_ipc.aidl.Book;
import com.example.binder_ipc.aidl.IBookManager;
import com.example.binder_ipc.aidl.IOnNewBookArrivedListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private IBookManager manager;
    private TextView textView;
    private Button button;
    private int number = 3;
    private List<Book> bookList = new ArrayList<>();
    private Book book;

    private IOnNewBookArrivedListener listener = new IOnNewBookArrivedListener.Stub(){

        @Override
        public void onNewBookArrived(Book book) throws RemoteException {
            Message message = handler.obtainMessage();
            message.obj = book;
            message.sendToTarget();
        }
    };

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            Book book = (Book) msg.obj;
            Toast.makeText(MainActivity.this,"A receive a new bookA:" + book.getBookName(),Toast.LENGTH_SHORT).show();
            super.handleMessage(msg);
        }
    };

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            manager = IBookManager.Stub.asInterface(service);
            try {
                manager.registerListener(listener);
                service.linkToDeath(deathRecipient,0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    private IBinder.DeathRecipient deathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            if (manager == null){
                return;
            }else {
                manager.asBinder().unlinkToDeath(deathRecipient,0);
                manager = null;
                Intent intent = new Intent();
                intent.setAction("com.server.aidl");
                intent.setPackage("com.example.binder_ipc");
                bindService(intent,connection,BIND_AUTO_CREATE);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.test);
        button = findViewById(R.id.button);
        button.setText("A");

        Intent intent = new Intent();
        intent.setAction("com.server.aidl");
        intent.setPackage("com.example.binder_ipc");
        bindService(intent,connection,BIND_AUTO_CREATE);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addBook();
            }
        });

//        Intent intent1 = new Intent(this,Main2Activity.class);
//        startActivity(intent1);
    }

    private void addBook() {
        book = new Book(number, "book" + number);
        Log.e(TAG,book.getBookName());
        try {
            manager.addBook(book);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        number++;
        Log.e(TAG,book.getBookName()+"");

//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    book = new Book(number, "book" + number);
//                    Log.e(TAG,book.getBookName());
//                    manager.addBook(book);
//                    number++;
//                    bookList = manager.getBookList();
//                } catch (RemoteException e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();
//
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                String text = "";
//                for (Book book : bookList){
//                    text = text + "\n" +"bookId: " + book.getBookId() + " bookName + " + book.getBookName();
//                    Log.d(TAG,text);
//                }
//                textView.setText(text);
//                Toast.makeText(MainActivity.this,book.getBookName(),Toast.LENGTH_LONG).show();
//            }
//        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(connection);
    }
}

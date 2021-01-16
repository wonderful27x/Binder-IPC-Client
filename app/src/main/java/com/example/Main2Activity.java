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

public class Main2Activity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private IBookManager manager;
    private TextView textView;
    private Button button;
    private int number = 3;
    private List<Book> bookList = new ArrayList<>();

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
            //Toast.makeText(Main2Activity.this,"B receive a new bookB:" + book.getBookName(),Toast.LENGTH_SHORT).show();
            super.handleMessage(msg);
        }
    };

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            manager = IBookManager.Stub.asInterface(service);
            try {
                manager.registerListener(listener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        textView = findViewById(R.id.test);
        button = findViewById(R.id.button);
        button.setText("B");

        Intent intent = new Intent();
        intent.setAction("com.server.aidl");
        intent.setPackage("com.example.binder_ipc");
        bindService(intent,connection,BIND_AUTO_CREATE);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addBook();
                getBooks();
                showBooks();
            }
        });
    }

    private void addBook() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Book book = new Book(number, "book" + number);
                    manager.addBook(book);
                    number++;
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }).start();

//        try {
//            Book book = new Book(number, "book" + number);
//            manager.addBook(book);
//            number++;
//        } catch (RemoteException e) {
//            e.printStackTrace();
//        }
    }

    private void getBooks(){
//不知道为什么开启线程会出问题
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    bookList = manager.getBookList();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }).start();
//        try {
//            bookList = manager.getBookList();
//        } catch (RemoteException e) {
//            e.printStackTrace();
//        }
    }

    private void showBooks(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String text = "start text";
                for (Book book : bookList){
                    text = text + "bookId: " + book.getBookId() + " bookName + " + book.getBookName()+"\n";
                    Log.d(TAG,"has book");
                }
                textView.setText(text);
            }
        });
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(connection);
    }
}

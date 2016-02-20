package co.tinode.tindroid;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;


import java.util.Timer;
import java.util.TimerTask;

import co.tinode.tinodesdk.Topic;
import co.tinode.tinodesdk.model.Description;
import co.tinode.tinodesdk.model.MsgServerData;
import co.tinode.tinodesdk.model.MsgServerInfo;
import co.tinode.tinodesdk.model.MsgServerMeta;
import co.tinode.tinodesdk.model.MsgServerPres;
import co.tinode.tinodesdk.model.Subscription;

import static co.tinode.tindroid.InmemoryCache.*;

/**
 * View to display a single conversation
 */
public class MessageActivity extends AppCompatActivity {

    private static final String TAG = "MessageActivity";

    // Delay before sending out a RECEIVED notification to be sure we are not sending too many.
    //private static final int RECEIVED_DELAY = 500;
    private static final int READ_DELAY = 1000;

    private String mTopicName;
    private Topic<VCard,String,String> mTopic;
    private MessagesListAdapter mMessagesAdapter;

    private Timer mNoteTimer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        mMessagesAdapter = new MessagesListAdapter(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MessageActivity.this, ContactsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        mTopicName = getIntent().getStringExtra("topic");
        ((TextView) findViewById(R.id.editMessage)).setText("");

        mTopic = (Topic<VCard,String,String>) getTinode().getTopic(mTopicName);
        if (mTopic != null) {
            VCard desc = mTopic.getPublic();
            toolbar.setTitle(desc.fn);
        } else {
            mTopic = new Topic<>(getTinode(), mTopicName,
                    new Topic.Listener<VCard,String,String>() {

                        @Override
                        public void onData(MsgServerData data) {
                            runOnUiThread(new Runnable() {
                                              @Override
                                              public void run() {
                                                  mMessagesAdapter.notifyDataSetChanged();
                                              }
                                          });

                            // Notify other subscribers that some messages were received
                            if (mNoteTimer == null) {
                                mNoteTimer = new Timer();
                                mNoteTimer.schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        mNoteTimer = null;
                                        mTopic.noteRead();
                                    }
                                }, READ_DELAY);
                            }
                        }

                        @Override
                        public void onPres(MsgServerPres pres) {

                        }

                        @Override
                        public void onInfo(MsgServerInfo info) {
                            switch (info.what) {
                                case "read":
                                case "recv":
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            mMessagesAdapter.notifyDataSetChanged();
                                        }
                                    });
                                    break;
                                case "kp":
                                    // TODO(gene): show typing notification
                                    break;
                                default:
                                    break;
                            }
                        }

                        @Override
                        public void onMeta(MsgServerMeta meta) {

                        }

                        @Override
                        public void onMetaSub(Subscription sub) {

                        }

                        @Override
                        public void onMetaDesc(final Description<VCard,String> desc) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (desc.pub != null) {
                                        toolbar.setTitle(" " + desc.pub.fn);

                                        desc.pub.constructBitmap();
                                        Bitmap bmp = desc.pub.getBitmap();
                                        if (bmp != null) {
                                            toolbar.setLogo(new RoundedImage(bmp));
                                        } else {
                                            Drawable icon;
                                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                                                icon = getDrawable(R.drawable.ic_account_circle_white);
                                            } else {
                                                icon = getResources().getDrawable(R.drawable.ic_account_circle_white);
                                            }
                                            toolbar.setLogo(icon);
                                        }
                                    }
                                }
                            });
                        }

                        @Override
                        public void onSubsUpdated() {

                        }
                    });
            getTinode().registerTopic(mTopic);
        }

        if (!mTopic.isSubscribed()) {
            try {
                mTopic.subscribe();
            } catch (Exception ex) {
                Log.e(TAG, "something went wrong", ex);
            }
        }

        ListView listViewMessages = (ListView) findViewById(R.id.messages_container);
        mMessagesAdapter.changeTopic(mTopicName);
        listViewMessages.setAdapter(mMessagesAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_topic_settings, menu);
        return true;
    }

    @Override
    protected void onNewIntent (Intent intent) {
        setIntent(intent);
    }

    public String getTopicName() {
        return mTopicName;
    }

    public MessagesListAdapter getMessagesAdapter() {
        return mMessagesAdapter;
    }
}
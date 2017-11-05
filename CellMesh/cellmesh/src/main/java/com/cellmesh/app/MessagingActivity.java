package com.cellmesh.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.cellmesh.app.model.INodeListener;
import com.cellmesh.app.model.Node;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public class MessagingActivity extends Activity implements INodeListener, View.OnClickListener
{
	private ListView peersTextView;
	private ListView chatTextView;
	private EditText Message;
	private ArrayList<String> messageList = new ArrayList<>();
	private ArrayAdapter<String> messageAdapter;

	private ArrayList<String> peerList = new ArrayList<>();
	private ArrayAdapter<String> peerAdapter;

	Node node;
	Map<Long,String> names;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		SharedPreferences sharedPref = this.getApplicationContext().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
		String name = sharedPref.getString(getString(R.string.pref_name), "");

		if ( name.equals("") ) {
			Intent intent = new Intent(MessagingActivity.this, MainActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
			startActivity(intent);
		}

		setContentView(R.layout.actvity_messaging);

		chatTextView = (ListView) findViewById(R.id.messagesListView);
		peersTextView = (ListView) findViewById(R.id.peersListView);

		peerAdapter= new ArrayAdapter<>(
				this,
				R.layout.peer_layout,
				peerList);
		messageAdapter = new ArrayAdapter<>(this,
				android.R.layout.simple_list_item_1,
				messageList);

		chatTextView.setAdapter(messageAdapter);
		peersTextView.setAdapter(peerAdapter);
		Message = (EditText) findViewById(R.id.message);

		//UI Must gather a name and create a listener before calling node.start
		node = new Node(MessagingActivity.this,this,name);
		names = node.getNamesMap();

		final Button Send_button = (Button) findViewById(R.id.Send);
		Message.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
				switch (i){
					case EditorInfo.IME_ACTION_DONE:
					case EditorInfo.IME_ACTION_GO:
					case EditorInfo.IME_ACTION_SEND:
					{
						Send_button.callOnClick();
					}
					default:
						if(keyEvent!=null){
							if(keyEvent.getKeyCode()==KeyEvent.KEYCODE_ENTER){
								textView.onEditorAction(EditorInfo.IME_ACTION_DONE);
							}
						}
				}
				return false;


			}
		});
		Send_button.setOnClickListener(this);
	}


	public void onClick(View v) {
		String message = Message.getText().toString();

		if ( message.equals("") ) {
			return;
		}

		BroadcastMessage(message);

		Message.setText("");
	}

	@Override
	protected void onStart()
	{
		super.onStart();
		if ( node != null ) {
			node.start();
		}
	}

	@Override
	protected void onStop()
	{
		super.onStop();

		if(node != null)
			node.stop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	public void onDisconected(){
		//remove from array of peers
	}
	public void onConnected(){
		//add to array of peers
	}

	public void BroadcastMessage(String message) {
		node.broadcastMessage(message);
	}

	@Override
	public void onEmergency(String newMessage, Long fromLinkId) {
		onDataReceived( "<b>" + newMessage + "</b>", fromLinkId);
	}

	@Override
	public void onNamesUpdated(Map<Long, String> names) {
		names = node.getNamesMap();
		updatePeerList();
	}

	public void updatePeerList() {
		peerList.clear();
		Set<Long> ids = node.getPeerIds();

		for ( Long id : ids ) {
			if ( names.get(id) != null ) {
				if ( !id.equals(node.getNodeId()) )
					peerList.add(names.get(id));
			} else {
				peerList.add("Unknown");
			}
		}

		peerAdapter.notifyDataSetChanged();
	}

	@Override
	public void onConnected(Set<Long> readOnlyIds, Long newId) {
		updatePeerList();
	}

	@Override
	public void onDisconnected(Set<Long> readOnlyIds, Long oldLinkId) {
		updatePeerList();
	}

	@Override
	public void onDataReceived(String newMessage, Long fromLinkId) {
		messageList.add(names.get(fromLinkId) + " ---> " + newMessage);
		messageAdapter.notifyDataSetChanged();
	}

	@Override
	public void onDataSent(String newMessage, Long fromLinkId) {
		onDataReceived(newMessage, fromLinkId);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings)
		{
			Intent intent = new Intent(MessagingActivity.this, MainActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
			String message = ""; //reset the name

			SharedPreferences sharedPref = MessagingActivity.this.getApplicationContext().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = sharedPref.edit();
			editor.putString(getString(R.string.pref_name), message);
			editor.apply();
			startActivity(intent);
			finish();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private static boolean started = false;

} // MainActivity

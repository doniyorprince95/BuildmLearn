package org.buildmlearn.learnfrommap.service;

import org.buildmlearn.learnfrommap.R;
import org.buildmlearn.learnfrommap.SplashActivity;
import org.buildmlearn.learnfrommap.databasehelper.Database;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

public class NotificationBarAlarm extends BroadcastReceiver {

	NotificationManager notifyManager;

	@Override
	public void onReceive(Context context, Intent intent) {

		Log.d("NotificationAlarm", "onReceive");
		Toast.makeText(context, "Notification Manager", Toast.LENGTH_SHORT).show();
		Intent intent1 = new Intent(context, SplashActivity.class);
		PendingIntent pIntent = PendingIntent.getActivity(context, 0, intent1, 0);
		Database db = new Database(context);
//		NotificationCompat.Builder mBuilder =
//				new NotificationCompat.Builder(context)
//		.setSmallIcon(R.drawable.ic_launcher)
//		.setContentTitle("Learn From Map")
//		.setContentIntent(pIntent)
//		.setContentText(db.getNotificationMsg());
//		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
//		.setSmallIcon(R.drawable.ic_launcher)
//		.setContentTitle("Learn From Map")
//		.setPriority(NotificationCompat.PRIORITY_MAX)
//		.setContentIntent(pIntent)
//		.setContentText("Events received");
//		NotificationCompat.InboxStyle inboxStyle =
//		new NotificationCompat.InboxStyle();
//		// Sets a title for the Inbox style big view
//		inboxStyle.setBigContentTitle("Learn From Map");
//		inboxStyle.addLine(db.getNotificationMsg());
//		inboxStyle.setSummaryText(db.getNotificationMsg());
		// Moves the big view style object into the notification object.
//		mBuilder.setStyle(inboxStyle);
		String message = db.getNotificationMsg();
		long[] pattern = {500,500,500,500,500,500,500,500,500};
		NotificationCompat.Builder builder = new NotificationCompat.Builder(
	            context);
	    Notification notification = builder.setContentIntent(pIntent)
	            .setSmallIcon(R.drawable.not).setTicker(message).setOnlyAlertOnce(true).setVibrate(pattern)
	            .setAutoCancel(true).setContentTitle("Learn From Map")
	            .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
	            .setContentText(message).setDefaults(Notification.DEFAULT_SOUND).build();

		db.close();
		NotificationManager mNotificationManager =
				(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(0, notification);
	}
}

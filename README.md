scale-imageview-android
=======================

Add pinch-in and pinch-out function to Android ImageVIew.

* Double-tap to enlarge
* Can pinch to zoom in or out on the view

Example Code

Very similar ImageView

Setting Layout XML

    <com.matabii.dev.scaleimageview.ScaleImageView
        android:id="@+id/image"
        android:layout_width="fill_parent"
        android:layout_height="fill_parent"
        android:src="@drawable/sample" />

or edit java source

        ScaleImageView image = (ScaleImageView) findViewById(R.id.image);
        Bitmap bitmap = BitmapFactory.decodeStream(is);
        image.setImageBitmap(bitmap);

======
Download the sample application

http://code.google.com/p/scale-imageview-android/downloads/detail?name=ScaleImageView.apk

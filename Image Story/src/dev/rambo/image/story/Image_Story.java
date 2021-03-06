package dev.rambo.image.story;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

public class Image_Story extends Activity {
	
	
	public MediaRecorder mrec = null;
	MediaPlayer mPlayer = new MediaPlayer();
	
	// button defines
	private ImageButton audio_Button = null;
	private ImageButton new_Button = null; 
	private ImageButton next_Button = null;
	private ImageButton prev_Button = null;
	private ImageButton delete_Button = null;
	private ImageButton save_Button = null;
	
	private ImageButton shift_Button = null;
	
	private ImageButton go_Button = null;
	
	// image view defines
	private ImageView mainImage = null;
	
	private ImageView rightThumbArr[] = new ImageView[4];
	private ImageView leftThumbArr[] = new ImageView[4];
	
	
	// constants
    private static final String AUDIO_RECORDER_FILE_EXT_3GP = ".3gp";
    private static final String AUDIO_RECORDER_FILE_EXT_MP4 = ".mp4";
	private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
	
	// Image Picking ID's
	private static final int IMAGE_PICK 	= 1;
	private static final int IMAGE_CAPTURE 	= 2;
	
	// Deletion Confirmation ID's
	private static final int ID_YES = 3;
	private static final int ID_NO = 4;
	
	// Audio ID's
	private static final int ID_RECORD = 5;
	private static final int ID_DELETE = 6;
	private static final int ID_PLAY = 7;
	
	// Shift Image ID's
	private static final int ID_LEFT = 8;
	private static final int ID_RIGHT = 9;
	
	private static int ITEM_SIZE = 1; // current size pointer
	private static final int MAX_ITEM_SIZE = 20; // number of item objects that can be created 
	
    private int currentFormat = 0;
    

    AudioImg[] items = new AudioImg[MAX_ITEM_SIZE]; // default 10 items
	private int item_count = 0;
    
	private int output_formats[] = { MediaRecorder.OutputFormat.MPEG_4, MediaRecorder.OutputFormat.THREE_GPP };
    private String file_exts[] = { AUDIO_RECORDER_FILE_EXT_MP4, AUDIO_RECORDER_FILE_EXT_3GP };
    
    
    
	private static final String TAG = "ImgStoryApp";
	static final String EXTRA_MESSAGE = "dev.rambo.image.story.AudioImageArray";
	
	// flag for button states
	boolean recFlag = false;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // try to recover old save?
        if (!getSave()){ // old save not found, init new story 
        	newStory();
        }else{
            // prevent errors from dodgey saves.. 
            if (items[item_count] == null){
            	items[item_count] = new AudioImg();
            }
              
            // check the size of the array before continuing
            ITEM_SIZE = 0;
            for(AudioImg a : items){
            	if (a != null){
            		ITEM_SIZE++;
            	}
            }
        }
        	
        	

        
        
        //sets up the popups for the buttons
        popSetup();
        
                
        // find the buttons and setup onclicks
        buttonSetup();
        
        // refresh the screen
        updateThumbs();
        
       
    }
    
    
    /**
     * finds the buttons and sets up their onclicks
     * @author Rambo
     */
    private void buttonSetup() {
    	try{
            
            next_Button = (ImageButton)findViewById(R.id.next);
            prev_Button = (ImageButton)findViewById(R.id.prev);
           
            save_Button = (ImageButton)findViewById(R.id.save);
            new_Button = (ImageButton)findViewById(R.id.newSlide);
            
            go_Button = (ImageButton)findViewById(R.id.playStory);
            
     
            // find imageviews
            rightThumbArr[0] = (ImageView)findViewById(R.id.rightImg1);
            rightThumbArr[1] = (ImageView)findViewById(R.id.rightImg2);
            rightThumbArr[2] = (ImageView)findViewById(R.id.rightImg3);
            rightThumbArr[3] = (ImageView)findViewById(R.id.rightImg4);
            leftThumbArr[0] = (ImageView)findViewById(R.id.leftImg1);
            leftThumbArr[1] = (ImageView)findViewById(R.id.leftImg2);
            leftThumbArr[2] = (ImageView)findViewById(R.id.leftImg3);
            leftThumbArr[3] = (ImageView)findViewById(R.id.leftImg4);
            } catch(Exception ex){ // report errors
            	Log.e(TAG, ":/ something went wrong finding the buttons, check your XML!");
            }
     
		
        // New button onclick
        // creates a new slide to the right of the current one if possible
        new_Button.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
            	InsertNewSlide();
            }
          });
        
        // Next button onclick
        // moves slides foward one position
        next_Button.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                // if not at the end of the array
            	if (item_count < ITEM_SIZE-1){
            		
            		item_count++; // move pointer foward one and
        
            		// Update thumbnails 
            		updateThumbs();
	
            		
            	} 
            }
          });
        
        // prev button onclick
        // moves slides back one position
        prev_Button.setOnClickListener(new View.OnClickListener(){      
        	public void onClick(View v) {
            	if (item_count > 0){ // if not at end
            		item_count--; // move pointer back one and 
            		// update the image thumbnails :D
            		updateThumbs();
            	}
            }
          });
        
        // save button onclick
        // opens the save manager
        save_Button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				
				saveManager();		
			}
		});
        
        // go button onclick
        // plays back the story
        go_Button.setOnClickListener(new View.OnClickListener(){      
        	public void onClick(View v) {
            	playShow();    	      
            }
        });
        
        // inital setup
        prev_Button.setEnabled(false); //cant go back yet
        
        if (ITEM_SIZE <= 1){ //if no item to the right then 
        	next_Button.setEnabled(false);
        }
        
        
        
        
	}


	/**
     * does the setup for the popup buttons
     * @author Rambo
     * 
     */
    private void popSetup(){

    	
    	 // Deletion Popup Options
    	ActionItem yesItem 	= new ActionItem(ID_YES, getResources().getDrawable(R.drawable.ok_app));
    	ActionItem noItem 	= new ActionItem(ID_NO, getResources().getDrawable(R.drawable.back_app));
    	
    	// Image picking popup options
    	ActionItem cameraItem = new ActionItem(IMAGE_CAPTURE, getResources().getDrawable(R.drawable.camera_app)); // , getResources().getDrawable(R.drawable.camera
    	ActionItem galleryItem = new ActionItem(IMAGE_PICK, getResources().getDrawable(R.drawable.gallery_app));
    	
    	// Audio popup options
    	ActionItem recordItem = new ActionItem(ID_RECORD, getResources().getDrawable(R.drawable.record_stop_audio_app));
    	recordItem.setSticky(true);
    	ActionItem deleteItem = new ActionItem(ID_DELETE, getResources().getDrawable(R.drawable.delete_audio_app));
    	ActionItem playItem = new ActionItem(ID_PLAY, getResources().getDrawable(R.drawable.play_audio_app));
    	
    	// Shift popup options
    	ActionItem leftItem = new ActionItem(ID_LEFT, getResources().getDrawable(R.drawable.left_app));
    	ActionItem rightItem = new ActionItem(ID_RIGHT, getResources().getDrawable(R.drawable.right_app));
    	
    	
    	// Deletion popup window
    	final QuickAction delAction 	= new QuickAction(this);
    	
    	// Camera popup window
    	final QuickAction picAction = new QuickAction(this);
    	
    	// Audio popup window
    	final QuickAction audAction = new QuickAction(this);
    	
    	// Shift popup window
    	final QuickAction sftAction = new QuickAction(this);
    	
    	
    	// Add options to deletion popup window
    	delAction.addActionItem(yesItem);
    	delAction.addActionItem(noItem);
    	
    	// Add options to image pick popup window
    	picAction.addActionItem(cameraItem);
    	picAction.addActionItem(galleryItem);
    	
    	// Add options to audio popup window
    	audAction.addActionItem(recordItem);
    	audAction.addActionItem(deleteItem);
    	audAction.addActionItem(playItem);
    	
    	// Add options to shift popup window
    	sftAction.addActionItem(leftItem);
    	sftAction.addActionItem(rightItem);
    	
    	
    	// Set options to delete 
    	delAction.setOnActionItemClickListener (new QuickAction.OnActionItemClickListener() {
    		public void onItemClick(QuickAction quickAction, int pos, int actionId) {
    			ActionItem actionItem = quickAction.getActionItem(pos);
    			
    			if (actionId == ID_YES) {
    				DeleteImage();
    				Toast.makeText(getApplicationContext(), "Deleted!", Toast.LENGTH_SHORT).show();
    			} else {
    				Toast.makeText(getApplicationContext(), actionItem.getTitle() + "Not Deleted!", Toast.LENGTH_SHORT).show();
    			}
    		}
    	});
    	
    	// Set options to image picker
    	picAction.setOnActionItemClickListener (new QuickAction.OnActionItemClickListener () {
    		public void onItemClick(QuickAction quickAction, int pos, int actionId) {
    			ActionItem actionItem = quickAction.getActionItem(pos);
    			
    			if (actionId == IMAGE_CAPTURE) {
    				Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
    				startActivityForResult(intent, IMAGE_CAPTURE);
    			} else {
    				Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
    				intent.setType("image/");
    				startActivityForResult(Intent.createChooser(intent, "Pick photo"), IMAGE_PICK);
    			}
    		}
    	});
    	
    	// Set options to audio
    	audAction.setOnActionItemClickListener (new QuickAction.OnActionItemClickListener() {
    		public void onItemClick(QuickAction quickAction, int pos, int actionId) {
    			ActionItem actionItem = quickAction.getActionItem(pos);
    			
    			if (actionId == ID_RECORD) {
    				if (!recFlag){ // start recording
    			           try
    			           {
    			        	   // delete the old file first if it exists?
    			        	   DeleteAudio();
    			        	   
    			        	   
    			        	   startRecording();
    			        	   Toast.makeText(getApplicationContext(), "Recording Audio...", Toast.LENGTH_SHORT).show();
    			        	   recFlag = true;
    			           }catch (Exception ee)
    			           {
    			        	   Log.e(TAG,"Caught io exception " + ee.getMessage());
    			           }
    	        	  } else { // stop recording

    	        		  stopRecording();
    	                  
    	        		  recFlag = false;
    	        		  Toast.makeText(getApplicationContext(), "Audio Recorded!", Toast.LENGTH_SHORT).show();
    	        
    	        	  }
    				Toast.makeText(getApplicationContext(), " Audio Recorded!", Toast.LENGTH_SHORT).show();
    			} else if (actionId == ID_DELETE) {
    				DeleteAudio();
    				Toast.makeText(getApplicationContext(), actionItem.getTitle() + "Audio Deleted!", Toast.LENGTH_SHORT).show();
    			} else { // Play audio
    				if(items[item_count].getAudio() != null){
    					try {
    						mPlayer.reset();
    						mPlayer.setDataSource(items[item_count].getAudio().getAbsolutePath());
    						mPlayer.prepare();
    						mPlayer.start();
    					} catch (Exception e) {
    						Log.e(TAG, "Error playing back audio.");
    					  }
    				}
    			}
    		}
    	});
    	
    	// Set options to shift
    	sftAction.setOnActionItemClickListener (new QuickAction.OnActionItemClickListener() {
    		public void onItemClick(QuickAction quickAction, int pos, int actionId) {
    			ActionItem actionItem = quickAction.getActionItem(pos);
    			
    			if (actionId == ID_LEFT) {
    				ShiftItemLeft();
    			} else {
    				ShiftItemRight();
    			}
    		}
    	});
    	
    	
    	// Link delete button
    	delete_Button = (ImageButton)findViewById(R.id.delete);
    	
    	// Link Image View to main picture
        mainImage = (ImageView)findViewById(R.id.mainImage);
        
        // Link audio button
        audio_Button = (ImageButton)findViewById(R.id.audio);
        
        // Link shift button
        shift_Button = (ImageButton)findViewById(R.id.shiftRight);
    	
        
    	// Delete button onClick
        delete_Button.setOnClickListener(new View.OnClickListener(){      
        	public void onClick(View v) {
            	delAction.show(v);       	      
            }
          });
        
        // Image View onClick
        mainImage.setOnClickListener(new View.OnClickListener(){
        	public void onClick(View v) {
        		picAction.show(v);
        	}
        });
        
        // Audio button onClick
        audio_Button.setOnClickListener(new View.OnClickListener(){
          public void onClick(View v) {
        	  audAction.show(v);
          }
        });
        
        // Shift button onClick
        shift_Button.setOnClickListener(new View.OnClickListener(){      
        	public void onClick(View v) {
            	sftAction.show(v);       	      
            }
          });
    }
    
    /**
     * starts a new activity to deal with teh save stuff...
     * @author Rambo
     */
    private void saveManager(){
    	Intent intent = new Intent(this, SaveMenu.class);
    	startActivity(intent);
    	
    }
    
    /**
     * tries to recover an old save
     * @return returns true if object recovered from save
     * @author Rambo
     */
    private boolean getSave(){
    	// try recover save?
        try{
	        FileInputStream inStream = openFileInput("save_file.dat");
	        ObjectInputStream objectInStream = new ObjectInputStream(inStream);
	        int count = objectInStream.readInt(); // Get the number of objects
	        
	        for (int c=0; c < count; c++)
	            items[c] = (AudioImg) objectInStream.readObject();
	        objectInStream.close();
	        
	        
	        
	        Toast toast = Toast.makeText(getApplicationContext(), "Save Restored!", Toast.LENGTH_SHORT);
	        toast.show();
	        
	        return true; // found save
	        
        } catch (Exception ex){ // something went wrong... 
        	
        	// report the error
        	Log.e(TAG, "save open error :(");
        	Log.e(TAG, ex.toString());
        	for (String s : fileList()){
        		Log.e(TAG, s);
        	}
        	
        	return false; // no save restored
        }
    }
    
    /**
     * saves the current story layout to a file
     * @author Rambo
     */
    private void saveStory(){
    	String FILENAME = "save_file.dat";
		try{
			FileOutputStream fos = openFileOutput(FILENAME, Context.MODE_PRIVATE);
			
			
			ObjectOutputStream objectOutStream = new ObjectOutputStream(fos);
			objectOutStream.writeInt(items.length); // Save size first
			for(AudioImg a :items)
			    objectOutStream.writeObject(a);
			objectOutStream.close();
			fos.close();
			Log.d(TAG, "File Saved!");
			
			// let the user know...
			Toast.makeText(getApplicationContext(), " Saved!", Toast.LENGTH_SHORT).show();
			
		} catch(Exception ex){
			Log.e(TAG, "Save error... :(");
		}
    }
    
    /**
     * initializes a new story
     * @author Rambo
     */
    private void newStory(){
    	// init the counters
    	item_count = 0;
    	ITEM_SIZE = 1; 
    	items[item_count] = new AudioImg(); // create a new object at the first position of the array
    	
    }
    
    
    /**
     * Little function to update all the thumbnail previews
     * @author Rambo
     */
    private void updateThumbs(){
    	try{
	    	// check if the main image exists
	    	if(items[item_count].fetchImg() != null){
				mainImage.setImageBitmap(items[item_count].fetchImg()); // update the view
				
			} else{
				mainImage.setImageResource(R.drawable.no_pic);
			}
	    	
	    	// right loop
	    	// first figure out how many values to process
	    	int num = 0;
	    	if (ITEM_SIZE - (item_count+1) > 4){ // more than 4 pics ahead
	    		num = 4;
	    	} else{ // only a few pics left
	    		num = ITEM_SIZE - (item_count+1) ;
	    	}
	    	
	    	for (int i = 0; i < num ; i++){ // update all dem piccies :D
	    		if (items[item_count+i+1] != null){
					if(items[item_count+i+1].fetchImg() != null){
						rightThumbArr[i].setImageBitmap(items[item_count+i+1].fetchImg());
					} else{
						rightThumbArr[i].setImageResource(R.drawable.no_pic);
					}
				} else{
					rightThumbArr[i].setImageResource(android.R.color.transparent);
				}
	    	}
	    	
	    	if ( 4 != num ){ // yoda says: deal with leftovers ;) 
	    		for (int i = 3; i >= num; i-- ){
	    			rightThumbArr[i].setImageResource(android.R.color.transparent);
	    		}
	    	}
	    	
	    	
	    	
	    	// left loop
	    	// first figure out how many values to process
	    	num = 0;
	    	if (item_count > 4){ // more than 4 pics behind
	    		num = 4;
	    	} else if( item_count != 4){ // only a few pics left
	    		num = item_count;
	    	} else{
	    		num = 3;
	    	}
	    	
	    	for (int i = 0; i < num ; i++){ // update all dem piccies :D
	    		if (items[item_count-i-1] != null){
					if(items[item_count-i-1].fetchImg() != null){
						leftThumbArr[i].setImageBitmap(items[item_count-i-1].fetchImg());
					} else{
						leftThumbArr[i].setImageResource(R.drawable.no_pic);
					}
				} else{
					leftThumbArr[i].setImageResource(android.R.color.transparent);
				}
	    	}
	    	
	    	if ( 4 != num ){ // yoda says: deal with leftovers ;) 
	    		for (int i = 3; i >= num; i-- ){
	    			leftThumbArr[i].setImageResource(android.R.color.transparent);
	    		}
	    	}
	    	
	    	
	    	// Deal with button logic
	    	if(item_count < ITEM_SIZE-1){
		    	next_Button.setEnabled(true);
	    	} else{
	    		next_Button.setEnabled(false);
	    	}
	    	
	    	if(item_count > 0){
	    		prev_Button.setEnabled(true);
	    	} else{
	    		prev_Button.setEnabled(false);
	    	}
	    	
	    	
	    	
			
    	} catch(Exception ex){
    		// just to catch any index errors?
    		Log.e(TAG, "Opps! index error!");
    	}
    	
    }
    
    
    protected void startRecording() throws IOException 
    {

		mrec = new MediaRecorder();   
    	mrec.setAudioSource(MediaRecorder.AudioSource.MIC);
        mrec.setOutputFormat(output_formats[currentFormat]);
        mrec.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        
        String file = getFilename();
        mrec.setOutputFile(file);
         
        File f = new File(file);
        items[item_count].setAudio(f); // store in object
        

        
        mrec.prepare();
        mrec.start();
        
    }

    protected void stopRecording() {
        mrec.stop();
        mrec.reset();
        mrec.release();
        mrec = null;
      }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    
    
    /**
     * want to insert a new slide
     * creates a new array, 1 item bigger than the last
     * and stick a new object in the current position
     */
    public void InsertNewSlide(){
    	if (ITEM_SIZE < MAX_ITEM_SIZE){ // only make bigger if space available
	    	
	    	AudioImg[] newItems = new AudioImg[++ITEM_SIZE]; // new array one position bigger
	    	
	    	// move left half of array
	    	for(int i = 0; i <= item_count; i++){
	    		newItems[i] = items[i];
	    	}
	    	
	    	// create new object
	    	newItems[item_count+1] = new AudioImg();
	    	
	    	// move right half of array
	    	if(item_count+1 < ITEM_SIZE-1){
		    	for(int i = item_count+1; i < ITEM_SIZE-1 ; i++){
		    		newItems[i+1] = items[i];
		    	}
	    	}
	    	
	    	items = newItems; // replace the old array
	    	updateThumbs(); // screen update
	    	
    	}
    }
    
    /**
     * swaps two objects if possible
     */
    public void ShiftItemRight(){
    	if (item_count < ITEM_SIZE-1){ // not at the end..
    		AudioImg temp = items[item_count+1]; // swap
    		items[item_count+1] = items[item_count];
    		items[item_count] = temp;
    		
    		item_count++; // move right
    		
    		updateThumbs();
    	} 
    }
    
    public void ShiftItemLeft(){
    	if (item_count > 0){ // not at the end..
    		AudioImg temp = items[item_count-1]; // swap
    		items[item_count-1] = items[item_count];
    		items[item_count] = temp;
    		
    		item_count--; // move left
    		
    		updateThumbs();
    	} 
    	
    }
    
    /**
     * creates a new activity that runs the show
     */
    public void playShow(){
    	Intent intent = new Intent(this, PlayStory.class);
    	saveStory();
    	startActivity(intent);
    	
    }
    
    /**
     * deletes the current image 
     */
    public void DeleteImage(){
    	if(items[item_count].getBitmapPath() != null){
	    	try{
		    	File imageFile = new File(items[item_count].getBitmapPath());
		    	if(imageFile != null){
		    		imageFile.delete();
		    	}
	    	} catch(Exception ex){
	    		Log.e(TAG, "Something horrible has gone wrong with the delete image function!!!");
	    		Log.e(TAG, ex.toString());
	    	}
	    	items[item_count].setBitmapPath(null);
	    }
    	
    	updateThumbs();
    	
    }
    
    /**
     * deletes the current audio file
     */
    public void DeleteAudio(){
       File audioFile = items[item_count].getAudio();
 	   if(audioFile != null){
 		   audioFile.delete();
 	   }
    	
    }
    
    
    private String getFilename(){ // generates the filename/path
    	String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,AUDIO_RECORDER_FOLDER);
       
        if(!file.exists()){
                file.mkdirs();
        }
       
        // save the audio file as the current timestamp
        return (file.getAbsolutePath() + "/" + System.currentTimeMillis() + file_exts[currentFormat]);
}
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();  // Always call the superclass method first
        
    	// not using anymore
        //String filepath = Environment.getExternalStorageDirectory().getPath();
        //File file = new File(filepath,AUDIO_RECORDER_FOLDER);
        //deleteDir(file);


    }
    
    //Deleting the temporary folder and the file created in the sdcard
    public static boolean deleteDir(File dir) 
    {
        if (dir.isDirectory()) 
        {
            String[] children = dir.list();
            for (int i=0; i<children.length; i++) 
            {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) 
                {
                    return false;
                }
            }
        }
        // The directory is now empty so delete it
        return dir.delete();
    }
    
    
    /**
     * Click listener for taking new picture
     * @author tscolari
     *
     */
    class TakePictureListener implements OnClickListener {
		public void onClick(View v) {
			Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
			startActivityForResult(intent, IMAGE_CAPTURE);
			
		}
    }
    
    /**
     * Receive the result from the startActivity
     */
    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	
    	if (resultCode == Activity.RESULT_OK) { 
	    	switch (requestCode) {
			case IMAGE_PICK:	
				this.imageFromGallery(resultCode, data);
				break;
			case IMAGE_CAPTURE:
				this.imageFromCamera(resultCode, data);
				break;
			default:
				break;
			}
    	}
    }
    
    /**
     * Image result from camera
     * @param resultCode
     * @param data
     */
    private void imageFromCamera(int resultCode, Intent data) {
    	this.updateImageView((Bitmap) data.getExtras().get("data"));
    }
    
    /**
     * Image result from gallery
     * @param resultCode
     * @param data
     */
    private void imageFromGallery(int resultCode, Intent data) {
    	Uri selectedImage = data.getData();
    	String [] filePathColumn = {MediaColumns.DATA};
    	
    	Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
    	cursor.moveToFirst();
    	
    	int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
    	String filePath = cursor.getString(columnIndex);
    	cursor.close();
    	
    	this.updateImageView(Bitmap.createScaledBitmap(BitmapFactory.decodeFile(filePath), 320, 240, false));
    }
    
    /**
     * Save the image to a file
     * Save the path to the object
     * Update the imageView with new bitmap
     * @param newImage
     */
    private void updateImageView(Bitmap newImage) {
    	
    	String path = getFilesDir().toString() + "/" + item_count + ".png";
    	
    	FileOutputStream out;
		try {
			
			out = new FileOutputStream(path);
			newImage.compress(CompressFormat.PNG, 100, out);
			out.close();
			
		} catch (FileNotFoundException e) {
			Log.e(TAG, "Something went wrong taking the pic and saving it... ");
			e.printStackTrace();
		} catch (IOException e) {
			Log.e(TAG, "Something went wrong closing the pic file output");
			e.printStackTrace();
		}
		items[item_count].setBitmapPath(path); // store the path to the new image
    	this.mainImage.setImageBitmap(items[item_count].fetchImg()); // update the view
    }


    
}

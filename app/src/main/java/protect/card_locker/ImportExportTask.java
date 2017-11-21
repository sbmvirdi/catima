package protect.card_locker;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

class ImportExportTask extends AsyncTask<Void, Void, Boolean>
{
    private static final String TAG = "LoyaltyCardLocker";

    private Activity activity;
    private boolean doImport;
    private DataFormat format;
    private File target;
    private InputStream inputStream;
    private TaskCompleteListener listener;

    private ProgressDialog progress;

    /**
     * Constructor which will setup a task for exporting to the given file
     */
    ImportExportTask(Activity activity, DataFormat format, File target,
            TaskCompleteListener listener)
    {
        super();
        this.activity = activity;
        this.doImport = false;
        this.format = format;
        this.target = target;
        this.listener = listener;
    }

    /**
     * Constructor which will setup a task for importing from the given InputStream.
     */
    ImportExportTask(Activity activity, DataFormat format, InputStream input,
                            TaskCompleteListener listener)
    {
        super();
        this.activity = activity;
        this.doImport = true;
        this.format = format;
        this.inputStream = input;
        this.listener = listener;
    }

    private boolean performImport(InputStream stream, DBHelper db)
    {
        boolean result = false;

        try
        {
            InputStreamReader reader = new InputStreamReader(stream, Charset.forName("UTF-8"));
            result = MultiFormatImporter.importData(db, reader, format);
            reader.close();
        }
        catch(IOException e)
        {
            Log.e(TAG, "Unable to import file", e);
        }

        Log.i(TAG, "Import result: " + result);

        return result;
    }

    private boolean performExport(File exportFile, DBHelper db)
    {
        boolean result = false;

        try
        {
            FileOutputStream fileWriter = new FileOutputStream(exportFile);
            OutputStreamWriter writer = new OutputStreamWriter(fileWriter, Charset.forName("UTF-8"));
            result = MultiFormatExporter.exportData(db, writer, format);
            writer.close();
        }
        catch (IOException e)
        {
            Log.e(TAG, "Unable to export file", e);
        }

        Log.i(TAG, "Export of '" + exportFile.getAbsolutePath() + "' result: " + result);

        return result;
    }

    protected void onPreExecute()
    {
        progress = new ProgressDialog(activity);
        progress.setTitle(doImport ? R.string.importing : R.string.exporting);

        progress.setOnDismissListener(new DialogInterface.OnDismissListener()
        {
            @Override
            public void onDismiss(DialogInterface dialog)
            {
                ImportExportTask.this.cancel(true);
            }
        });

        progress.show();
    }

    protected Boolean doInBackground(Void... nothing)
    {
        final DBHelper db = new DBHelper(activity);
        boolean result;

        if(doImport)
        {
            result = performImport(inputStream, db);
        }
        else
        {
            result = performExport(target, db);
        }

        return result;
    }

    protected void onPostExecute(Boolean result)
    {
        listener.onTaskComplete(result);

        progress.dismiss();
        Log.i(TAG, (doImport ? "Import" : "Export") + " Complete");
    }

    protected void onCancelled()
    {
        progress.dismiss();
        Log.i(TAG, (doImport ? "Import" : "Export") + " Cancelled");
    }
    interface TaskCompleteListener
    {
        void onTaskComplete(boolean success);
    }

}

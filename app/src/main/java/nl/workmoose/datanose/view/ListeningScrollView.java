package nl.workmoose.datanose.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

/**
 * Class that adds the possibility to add a OnScrollChangedListener
 */
public class ListeningScrollView extends ScrollView {

    private OnScrollChangedListener mOnScrollChangedListener;

    public ListeningScrollView(Context context) {
        super(context);
    }

    public ListeningScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setOnScrollChangedListener(OnScrollChangedListener listener) {
        mOnScrollChangedListener = listener;
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (mOnScrollChangedListener != null) {
            mOnScrollChangedListener.onScrollChanged(t);
        }
    }

    public interface OnScrollChangedListener {
        void onScrollChanged(int y);
    }
}

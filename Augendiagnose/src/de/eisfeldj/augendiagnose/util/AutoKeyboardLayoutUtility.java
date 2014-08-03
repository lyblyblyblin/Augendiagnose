package de.eisfeldj.augendiagnose.util;

import android.app.Activity;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

/**
 * Utility class that ensures that the activity is resized when the keyboard appears, in full screen mode.
 *
 * android:windowSoftInputMode="adjustResize" does not work in full screen mode.
 *
 * Thanks to Joseph Johnson for this class.
 *
 * For more information, see https://code.google.com/p/android/issues/detail?id=5497 To use this class, simply invoke
 * assistActivity() on an Activity that already has its content view set.
 */
public final class AutoKeyboardLayoutUtility {

	/**
	 * Assumed minimum portion that the keyboard fills vertically.
	 */
	private static final float MIN_KEYBOARD_SIZE = .1f;

	/**
	 * Method to be called to apply the workaround to the activity. Should be called at the end of onCreate().
	 *
	 * @param activity
	 *            the activity which uses the workaround.
	 */
	@SuppressWarnings("unused")
	public static void assistActivity(final Activity activity) {
		new AutoKeyboardLayoutUtility(activity);
	}

	/**
	 * Method to be called to apply the workaround to the activity. Should be called at the end of onCreate().
	 *
	 * @param activity
	 *            the activity which uses the workaround.
	 * @param callback
	 *            a callback to be called if the kayboard is shown or hidden.
	 * @param changeLayout
	 *            Flag indicating if the layout should be changed by this tool, or if it is only used for the callback.
	 */
	public static void assistActivity(final Activity activity, final OnKeyboardChangeListener callback,
			final boolean changeLayout) {
		AutoKeyboardLayoutUtility instance = new AutoKeyboardLayoutUtility(activity);
		instance.callback = callback;
		instance.changeLayout = changeLayout;
	}

	// JAVADOC:OFF
	private View mChildOfContent;
	private int usableHeightPrevious;
	private FrameLayout.LayoutParams frameLayoutParams;
	private ActivityWithExplicitLayoutTrigger activityWithLayoutTrigger = null;

	// JAVADOC:ON

	/**
	 * A callback to be called if the kayboard is shown or hidden.
	 */
	private OnKeyboardChangeListener callback = null;

	/**
	 * Flag indicating if the layout should be changed by this tool, or if it is only used for the callback.
	 */
	private boolean changeLayout = true;

	/**
	 * Constructor, adding a listener to change the global layout if required.
	 *
	 * @param activity
	 *            the activity which uses the workaround.
	 */
	private AutoKeyboardLayoutUtility(final Activity activity) {
		FrameLayout content = (FrameLayout) activity.findViewById(android.R.id.content);
		mChildOfContent = content.getChildAt(0);
		mChildOfContent.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				possiblyResizeChildOfContent();
			}
		});
		frameLayoutParams = (FrameLayout.LayoutParams) mChildOfContent.getLayoutParams();

		if (activity instanceof ActivityWithExplicitLayoutTrigger) {
			activityWithLayoutTrigger = (ActivityWithExplicitLayoutTrigger) activity;
		}
	}

	/**
	 * Resize the view, as the keyboard may have appeared or vanished.
	 */
	private void possiblyResizeChildOfContent() {
		int usableHeightNow = computeUsableHeight();
		if (usableHeightNow != usableHeightPrevious) {
			int usableHeightSansKeyboard = mChildOfContent.getRootView().getHeight();
			int heightDifference = usableHeightSansKeyboard - usableHeightNow;
			if (heightDifference > (usableHeightSansKeyboard * MIN_KEYBOARD_SIZE)) {
				// keyboard probably just became visible
				if (changeLayout) {
					frameLayoutParams.height = usableHeightSansKeyboard - heightDifference;
				}
				if (callback != null) {
					callback.onKeyboardChanged(true);
				}
			}
			else {
				// keyboard probably just became hidden
				if (changeLayout) {
					frameLayoutParams.height = usableHeightSansKeyboard;
				}
				if (callback != null) {
					callback.onKeyboardChanged(false);
				}
			}
			mChildOfContent.requestLayout();
			usableHeightPrevious = usableHeightNow;

			if (activityWithLayoutTrigger != null) {
				activityWithLayoutTrigger.requestLayout();
			}
		}
	}

	/**
	 * Calculate the height that can be used for the view above keyboard.
	 *
	 * @return the usable height.
	 */
	private int computeUsableHeight() {
		Rect r = new Rect();
		mChildOfContent.getWindowVisibleDisplayFrame(r);
		return r.bottom - r.top;
	}

	/**
	 * Callback for activities to request layout.
	 */
	public interface ActivityWithExplicitLayoutTrigger {
		/**
		 * Callback method to do request layout.
		 */
		void requestLayout();
	}

	/**
	 * Callback listener that will be informed if the keyboard is added or removed.
	 */
	public interface OnKeyboardChangeListener {
		/**
		 * Callback method that will be called if the keyboard is added or removed.
		 *
		 * @param visible
		 *            true if the keyboard is added, false if the keyboard is removed.
		 */
		void onKeyboardChanged(final boolean visible);
	}
}

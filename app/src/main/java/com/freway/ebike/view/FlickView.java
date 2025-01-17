/*
 * Copyright (c) 2010-2011, The MiCode Open Source Community (www.micode.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.freway.ebike.view;

import com.freway.ebike.R;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 自定义一个View继承TextView
 * 
 * 
 */
public class FlickView extends LinearLayout {
	private Animation animation;
	private Animation alphAnim;
	public boolean isAnimating=false;
	private View tipView;
	public FlickView(Context context) {
		super(context);
		initView();
	}

	public FlickView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView();
	}

	public FlickView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initView();
	}

	private void initView(){
		tipView=findViewById(R.id.travel_tip_ll);
	}
	
	private Handler handler=new Handler(){

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if(isAnimating){
				clearAnimation();
				isAnimating=false;
			}
		}
		
	};
	
	public void showTip() {
//		handler.removeMessages(0);
//		handler.sendEmptyMessageDelayed(0, 5000);
		if(getVisibility()!=View.VISIBLE){
			setVisibility(View.VISIBLE);
			if(alphAnim==null){
				alphAnim=new AlphaAnimation(0, 1);
				alphAnim.setDuration(500);
				alphAnim.setFillAfter(false);
			}
			startAnimation(alphAnim);
		}
		if(tipView==null){
			tipView=findViewById(R.id.travel_tip_ll);
		}
		if(!isAnimating){
			if (animation == null) {
				animation = new AlphaAnimation(1, 0);
				animation.setDuration(500);
				animation.setRepeatCount(Animation.INFINITE);
				animation.setRepeatMode(Animation.REVERSE);

			}
			if(tipView!=null){
				tipView.clearAnimation();
				tipView.startAnimation(animation);
			}
			isAnimating=true;
		}
	}

	public void hideTip() {
		if(tipView==null){
			tipView=findViewById(R.id.travel_tip_ll);
		}
		if(getVisibility()==View.VISIBLE){
			setVisibility(View.GONE);
		}
		if(isAnimating){
			if(tipView!=null){
				tipView.clearAnimation();
			}
			isAnimating=false;
		}
	}
}

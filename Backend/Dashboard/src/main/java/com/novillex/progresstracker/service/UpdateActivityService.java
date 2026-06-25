package com.novillex.progresstracker.service;

import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.model.ActivityModel;
import com.novillex.progresstracker.model.ActivityUpdateRequestModel;

public interface UpdateActivityService {
	
//	Response updateActivity(ActivityModel activity);
	
	Response updateActivityRequest(ActivityUpdateRequestModel request);
}

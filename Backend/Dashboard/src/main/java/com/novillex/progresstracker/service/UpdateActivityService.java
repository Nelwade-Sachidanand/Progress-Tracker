package com.novillex.progresstracker.service;

import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.model.ActivityModel;
import com.novillex.progresstracker.model.ActivityUpdateRequestModel;
import com.novillex.progresstracker.model.AddRemarkModel;

public interface UpdateActivityService {
		
	Response updateActivityRequest(ActivityUpdateRequestModel activityUpdateRequestModel);
	
	Response addRemark(AddRemarkModel addRemarkModel);
}

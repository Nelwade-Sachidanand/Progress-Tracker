package com.novillex.progresstracker.service;

import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.model.ActivityModel;

public interface UpdateActivityService {
	
	Response updateActivity(ActivityModel activity);
}

package com.novillex.progresstracker.model;

import com.novillex.progresstracker.entity.Activity;
import com.novillex.progresstracker.entity.Milestone;
import com.novillex.progresstracker.entity.Phase;
import com.novillex.progresstracker.entity.Subtask;
import com.novillex.progresstracker.entity.Task;

import lombok.Data;

@Data
public class HierarchyReference {

    private Phase phase;

    private Milestone milestone;

    private Task task;

    private Subtask subTask;

    private Activity activity;

}
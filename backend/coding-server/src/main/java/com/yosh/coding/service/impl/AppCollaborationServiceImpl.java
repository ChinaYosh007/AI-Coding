package com.yosh.coding.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.yosh.coding.mapper.AppCollaborationMapper;
import com.yosh.coding.service.AppCollaborationService;
import com.yosh.model.entity.AppCollaboration;
import org.springframework.stereotype.Service;

@Service
public class AppCollaborationServiceImpl extends ServiceImpl<AppCollaborationMapper, AppCollaboration> implements AppCollaborationService {
}
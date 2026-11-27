package com.kanbanflow.controller;

import com.kanbanflow.dto.CodeResponse;
import com.kanbanflow.service.ColumnService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CodeController {

    private final ColumnService columnService;

    public CodeController(ColumnService columnService) {
        this.columnService = columnService;
    }

    /** 사용 중(USE_YN='Y')인 코드 목록. 컬럼 추가 UI용 (예: STATUS000). */
    @GetMapping("/api/codes/{groupCd}")
    public List<CodeResponse> codes(@PathVariable String groupCd) {
        return columnService.codes(groupCd);
    }
}

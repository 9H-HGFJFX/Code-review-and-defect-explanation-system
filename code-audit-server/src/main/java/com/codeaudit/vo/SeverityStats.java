package com.codeaudit.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeverityStats {
    private long critical;
    private long error;
    private long warning;
    private long suggestion;
    private long total;
}

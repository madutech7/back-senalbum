package com.senalbum.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AlbumStatsResponse {
    private long viewCount;
    private long downloadCount;
}

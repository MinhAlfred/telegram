package thitkho.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.function.Function;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CursorPage<T> {
    private List<T> data;
    private String nextCursor;
    private boolean hasMore;

    public static <T> CursorPage<T> of(List<T> data, int limit,
                                       Function<T, String> cursorExtractor) {
        boolean hasMore = data.size() == limit;
        String nextCursor = hasMore ? cursorExtractor.apply(data.getLast()) : null;
        return new CursorPage<>(data, nextCursor, hasMore);
    }
}

# Null Safety 컨벤션

## 사용 라이브러리

**JSpecify** (`org.jspecify.annotations`)

Spring Framework 7부터 JSpecify를 공식 채택했다.
기존에 난립하던 null 어노테이션들을 대체한다.

```
org.springframework.lang.Nullable     → org.jspecify.annotations.Nullable
org.springframework.lang.NonNull      → @NullMarked 클래스 선언으로 대체
javax.annotation.Nullable             → 사용 금지 (JSR-305, 유지보수 중단)
org.jetbrains.annotations.Nullable    → 사용 금지 (IDE 전용)
```

## 이 프로젝트에서의 규칙

- null 어노테이션은 **`org.jspecify.annotations`만 사용**한다.
- 별도 의존성 추가 불필요 — Spring Framework 7의 이행적 종속성으로 이미 포함되어 있다.

## 주요 어노테이션

| 어노테이션 | 의미 | 적용 대상 |
|-----------|------|-----------|
| `@NullMarked` | 이 범위 안의 모든 타입은 기본 non-null | 클래스, 패키지 |
| `@Nullable` | 이 타입은 null 허용 | 파라미터, 반환 타입, 필드 |

## 사용 예시

```java
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class MyService {

    // name은 non-null (클래스에 @NullMarked)
    // 반환은 null 가능
    public @Nullable String findName(String id) {
        return repository.findById(id)
                .map(Entity::getName)
                .orElse(null);
    }
}
```

## Spring 7 메서드 오버라이드 시 주의

Spring 7의 부모 클래스가 `@NullMarked`로 선언되어 있으므로,
오버라이드하는 자식 클래스에도 `@NullMarked`를 붙여야 경고가 안 뜬다.

```java
// WebAppInitializer 예시
@NullMarked
public class WebAppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

    @Override
    protected Class<?> @Nullable [] getRootConfigClasses() {
        return new Class[]{AppConfig.class};
    }
}
```

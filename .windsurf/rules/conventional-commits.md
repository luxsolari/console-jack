---
trigger: model_decision
description: Conventional commits rules to apply when AI is requested to create commits, or do git operations that involve commiting, pushing and otherwhise working with Git commits.
globs: 
---

# Conventional Commits Ruleset for AI Agents

Based on the official Conventional Commits v1.0.0 specification, this is a complete, practical ruleset for AI agents to generate compliant commit messages.

## Core Format Structure

**Every commit MUST follow this exact format:**

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

**Required elements:** type, colon, space, description  
**Optional elements:** scope, exclamation mark, body, footer(s)

## Commit Types Reference

### Required Types
- **`feat`** - MUST use for new features (triggers MINOR version)
- **`fix`** - MUST use for bug fixes (triggers PATCH version)

### Recommended Additional Types
- **`build`** - Changes affecting build system or external dependencies
- **`chore`** - Maintenance tasks, no production code change  
- **`ci`** - Changes to CI configuration files and scripts
- **`docs`** - Documentation only changes
- **`perf`** - Code changes that improve performance
- **`refactor`** - Code changes that neither fix bugs nor add features
- **`revert`** - Reverts a previous commit
- **`style`** - Changes that don't affect code meaning (formatting, whitespace)
- **`test`** - Adding or correcting tests

### AI Agent Type Selection Rules
1. If adding new functionality → use `feat`
2. If fixing a bug or issue → use `fix`
3. If changing documentation only → use `docs`
4. If changing tests only → use `test`
5. If improving performance → use `perf`
6. If restructuring without behavior change → use `refactor`
7. If changing build/CI → use `build` or `ci`
8. If neither adding features nor fixing bugs → use `chore`

## Scope Usage Rules

**Format:** `(scope)` - noun in parentheses after type

**Examples:**
- `feat(parser): add ability to parse arrays`
- `fix(api): resolve authentication timeout`  
- `docs(readme): update installation instructions`
- `test(auth): add login validation tests`

**AI Agent Scope Guidelines:**
- Use scope when changes affect a specific module, component, or area
- Choose descriptive nouns: `api`, `parser`, `auth`, `ui`, `database`
- Omit scope for broad changes affecting multiple areas
- Keep scopes consistent within a project

## Breaking Changes Notation

**Two methods to indicate breaking changes:**

### Method 1: Exclamation Mark
```
feat!: send email when product ships
feat(api)!: remove deprecated endpoint
```

### Method 2: Footer
```
feat: allow config object extension

BREAKING CHANGE: `extends` key now used for extending config files
```

**AI Agent Breaking Change Rules:**
1. **Always** indicate breaking changes using one method
2. Use exclamation mark for simple breaking changes
3. Use footer for breaking changes requiring detailed explanation
4. Breaking changes trigger MAJOR version bumps regardless of type

## Footer Formats

**Structure:** `<token><separator><value>`

**Valid separators:**
- `: ` (colon space)
- ` #` (space hash)

**Common footer tokens:**
- `BREAKING CHANGE: description`
- `Refs: #123`
- `Fixes: #456`
- `Closes: #789`
- `Co-authored-by: Name <email@example.com>`
- `Reviewed-by: Name <email@example.com>`

**AI Agent Footer Rules:**
1. Use `Fixes: #issue` for bug fix commits
2. Use `Refs: #issue` for feature commits referencing issues
3. Use `Closes: #issue` when commit fully resolves an issue
4. Replace spaces with `-` in multi-word tokens: `Reviewed-by`
5. Place footers after blank line following body

## Multi-line Message Format

**Body guidelines:**
- Start body one blank line after description
- Use free-form paragraphs separated by blank lines
- Provide additional context, reasoning, or implementation details
- Explain **why** the change was made, not just **what** changed

**Example:**
```
fix: prevent racing of requests

Introduce request ID and reference to latest request. Dismiss
incoming responses other than from latest request.

Remove timeouts which were used to mitigate racing but are
obsolete now.

Fixes: #123
```

## AI Agent Decision Tree

**When generating commits, follow this logic:**

```
1. Identify change type
   ├─ New feature? → feat
   ├─ Bug fix? → fix
   ├─ Docs only? → docs
   ├─ Tests only? → test
   ├─ Performance? → perf
   ├─ Refactor? → refactor
   └─ Other? → chore

2. Determine scope
   ├─ Affects specific component? → Add (component)
   └─ Broad change? → No scope

3. Check breaking changes
   ├─ API changes? → Add ! or footer
   ├─ Config changes? → Add ! or footer
   └─ Backward compatible? → No indicator

4. Add issue references
   ├─ Fixes bug? → Fixes: #123
   ├─ Implements feature? → Refs: #123
   └─ No issue? → No footer
```

## Scenario-Specific Rules

### Hotfixes
```
fix: resolve critical authentication bypass

Security vulnerability allowing unauthorized access through
malformed tokens.

Fixes: #SECURITY-001
```

### Release Preparation
```
chore: bump version to 2.1.0

Update package.json and CHANGELOG for release.
```

### Dependency Updates
```
build: update lodash to 4.17.21

Security update addressing prototype pollution vulnerability.

Refs: #456
```

### Reverts
```
revert: remove experimental caching feature

This reverts commit abc123def456. Feature caused memory leaks
in production environment.

Refs: #789
```

### Configuration Changes
```
feat!: migrate to new config format

BREAKING CHANGE: Configuration files must now use YAML format
instead of JSON. Run migration script to convert existing configs.
```

## Examples of Good vs Bad Commits

### ✅ Good Examples
```
feat(auth): add OAuth2 integration
fix: resolve memory leak in event handlers
docs: update API documentation for v2.0
test(parser): add edge case validation
perf(database): optimize query performance
refactor(utils): extract common validation logic
chore: update development dependencies
```

### ❌ Bad Examples
```
Fixed stuff                           # No type, vague
feat add new feature                  # Missing colon
Feat: Add New Feature                 # Wrong capitalization  
fix(API): Fix Bug                     # Description too vague
feat: implement new feature.          # Unnecessary period
FEAT: new feature                     # Type should be lowercase
fix : memory leak                     # Extra space before colon
```

## AI Agent Validation Checklist

Before generating a commit, verify:

**Format Requirements:**
- [ ] Type is present and lowercase
- [ ] Colon immediately follows type/scope
- [ ] Single space after colon
- [ ] Description starts with lowercase verb
- [ ] No period at end of description
- [ ] Breaking change properly indicated
- [ ] Footers use correct token format

**Content Requirements:**
- [ ] Type matches the actual change
- [ ] Description is clear and specific
- [ ] Scope is relevant if used
- [ ] Body explains reasoning when needed
- [ ] Issue references are accurate

## Special AI Agent Considerations

### Length Limits
- **Description:** Keep under 50 characters when possible, max 72
- **Body lines:** Wrap at 72 characters
- **Total message:** No hard limit, but be concise

### Language Rules
- Use imperative mood: "add feature" not "added feature"
- Start description with lowercase letter
- Don't end description with period
- Use present tense in body: "This change does" not "This change did"

### Consistency Guidelines
- Maintain consistent type usage within projects
- Use same scope names for similar components
- Follow team conventions for additional types
- Be consistent with breaking change notation style

### Error Prevention
- Never use both `!` and space before colon
- Always use blank line between sections
- Don't abbreviate in ways that reduce clarity
- Avoid subjective language ("improve", "enhance")
- Use specific technical terms over generic ones

This ruleset provides comprehensive guidance for AI agents to generate Conventional Commits v1.0.0 compliant messages that are clear, consistent, and actionable.

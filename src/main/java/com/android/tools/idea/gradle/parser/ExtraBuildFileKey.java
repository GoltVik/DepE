package com.android.tools.idea.gradle.parser;

import com.android.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElement;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;

import java.util.List;

import static com.android.tools.idea.gradle.parser.BuildFileKeyType.CLOSURE;
import static com.android.tools.idea.gradle.parser.ValueFactory.KeyFilter;

public enum ExtraBuildFileKey {

    EXT("ext", "EXTRA", CLOSURE, Dependency.getFactory());

    private final String myPath;
    private final BuildFileKeyType myType;
    private final ValueFactory myValueFactory;
    private final String myDisplayName;

    ExtraBuildFileKey(@NotNull String path, @Nullable String displayName, @NotNull BuildFileKeyType type, @Nullable ValueFactory factory) {
        myPath = path;
        myType = type;
        myValueFactory = factory;

        if (displayName != null) {
            myDisplayName = displayName;
        } else {
            int lastSlash = myPath.lastIndexOf('/');
            myDisplayName = splitCamelCase(lastSlash >= 0 ? myPath.substring(lastSlash + 1) : myPath);
        }
    }

    @NotNull
    @VisibleForTesting
    static String splitCamelCase(@NotNull String string) {
        StringBuilder sb = new StringBuilder(2 * string.length());
        int n = string.length();
        boolean lastWasUpperCase = Character.isUpperCase(string.charAt(0));
        boolean capitalizeNext = true;
        for (int i = 0; i < n; i++) {
            char c = string.charAt(i);
            boolean isUpperCase = Character.isUpperCase(c);
            if (isUpperCase && !lastWasUpperCase) {
                sb.append(' ');
                capitalizeNext = true;
            }
            lastWasUpperCase = isUpperCase;
            if (capitalizeNext) {
                c = Character.toUpperCase(c);
                capitalizeNext = false;
            } else {
                c = Character.toLowerCase(c);
            }
            sb.append(c);
        }

        return sb.toString();
    }

    @NotNull
    public BuildFileKeyType getType() {
        return myType;
    }

    @NotNull
    public String getPath() {
        return myPath;
    }

    public boolean shouldInsertAtBeginning() {
        return false;
    }

    @Nullable
    protected Object getValue(@NotNull GroovyPsiElement arg) {
        if (myValueFactory != null && arg instanceof GrClosableBlock) {
            return myValueFactory.getValues((GrClosableBlock) arg);
        }
        return myType.getValue(arg);
    }

    @SuppressWarnings("unchecked")
    protected void setValue(@NotNull GroovyPsiElement arg, @NotNull Object value, @Nullable KeyFilter filter) {
        if (myValueFactory != null && arg instanceof GrClosableBlock && value instanceof List) {
            myValueFactory.setValues((GrClosableBlock) arg, (List) value, filter);
        } else {
            myType.setValue(arg, value);
        }
    }

}

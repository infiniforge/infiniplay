#!/usr/bin/env python3
"""Apply the app-owned optional N64 bob shader to the generated host renderer."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
path = ROOT / "build/native-source/video.cpp"
s = path.read_text()

def replace(old, new, count=1):
    global s
    actual = s.count(old)
    assert actual == count, f"Expected {count} occurrence(s), found {actual}: {old[:80]!r}"
    s = s.replace(old, new, count)

replace(
    "namespace libretrodroid {",
    "namespace libretrodroid {\n\nstatic bool playN64BobShaderLoaded = false;",
)
replace(
    """    if (loadedShaderType.has_value() && loadedShaderType.value() == requestedShaderConfig) {
        return;
    }

    loadedShaderType = requestedShaderConfig;

    auto shaders = ShaderManager::getShader(requestedShaderConfig);""",
    """    if (loadedShaderType.has_value() && loadedShaderType.value() == requestedShaderConfig &&
        playN64BobShaderLoaded == playN64BobEnabled) {
        return;
    }

    loadedShaderType = requestedShaderConfig;
    playN64BobShaderLoaded = playN64BobEnabled;

    auto shaders = ShaderManager::getShader(requestedShaderConfig);""",
)
bob = r'''    if (playN64BobEnabled) {
        static const std::string bobVertex =
            "attribute vec4 vPosition;\n"
            "attribute vec2 vCoordinate;\n"
            "varying mediump vec2 coords;\n"
            "void main() { coords = vCoordinate; gl_Position = vPosition; }\n";
        static const std::string bobFragment =
            "#ifdef GL_FRAGMENT_PRECISION_HIGH\n"
            "#define HIGHP highp\n"
            "#else\n"
            "#define HIGHP mediump\n"
            "precision mediump float;\n"
            "#endif\n"
            "uniform lowp sampler2D previousPass;\n"
            "uniform HIGHP vec2 textureSize;\n"
            "uniform HIGHP float n64FieldParity;\n"
            "varying mediump vec2 coords;\n"
            "void main() {\n"
            "  HIGHP float h = max(textureSize.y, 2.0);\n"
            "  HIGHP float y = clamp(coords.y * h - 0.5, 0.0, h - 1.0);\n"
            "  HIGHP float parity = mod(n64FieldParity, 2.0);\n"
            "  HIGHP float y0 = clamp(floor((y - parity) * 0.5) * 2.0 + parity, 0.0, h - 1.0);\n"
            "  HIGHP float y1 = clamp(y0 + 2.0, 0.0, h - 1.0);\n"
            "  HIGHP float blend = clamp((y - y0) * 0.5, 0.0, 1.0);\n"
            "  HIGHP vec2 uv0 = vec2(coords.x, (y0 + 0.5) / h);\n"
            "  HIGHP vec2 uv1 = vec2(coords.x, (y1 + 0.5) / h);\n"
            "  lowp vec3 a = texture2D(previousPass, uv0).rgb;\n"
            "  lowp vec3 b = texture2D(previousPass, uv1).rgb;\n"
            "  gl_FragColor = vec4(mix(a, b, blend), 1.0);\n"
            "}\n";
        shaders.passes.push_back({bobVertex, bobFragment, false, 1.0f});
    }
'''
replace(
    "    auto shaders = ShaderManager::getShader(requestedShaderConfig);\n\n    shadersChain = {};",
    "    auto shaders = ShaderManager::getShader(requestedShaderConfig);\n\n" + bob + "\n    shadersChain = {};",
)
replace(
    "\n}\n\nfloat Video::getScreenDensity()",
    "\n    playN64BobAdvanceField();\n}\n\nfloat Video::getScreenDensity()",
)
replace(
    "        glUniform1f(shader.gScreenDensityHandle, getScreenDensity());",
    """        glUniform1f(shader.gScreenDensityHandle, getScreenDensity());
        if (playN64BobEnabled) {
            const GLint parityHandle = glGetUniformLocation(shader.gProgram, "n64FieldParity");
            if (parityHandle != -1) {
                glUniform1f(parityHandle, static_cast<float>(playN64BobFieldParity));
                if (i > 0) {
                    const auto previousPass = renderer->getPassData(i - 1);
                    glUniform2f(
                        shader.gTextureSizeHandle,
                        static_cast<float>(previousPass.width.value_or(
                            static_cast<unsigned int>(getTextureWidth())
                        )),
                        static_cast<float>(previousPass.height.value_or(
                            static_cast<unsigned int>(getTextureHeight())
                        ))
                    );
                }
            }
        }""",
)
replace(
    """        renderer->onNewFrame(data, width, height, pitch);
        isDirty = true;""",
    """        renderer->onNewFrame(data, width, height, pitch);
        isDirty = true;
        playN64BobMarkFrame();""",
)
path.write_text(s)

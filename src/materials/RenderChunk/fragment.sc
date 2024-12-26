$input v_color0, v_color1, v_fog, v_refl, v_texcoord0, v_lightmapUV, v_extra

#include <bgfx_shader.sh>
#include <newb/main.sh>

SAMPLER2D_AUTOREG(s_MatTexture);
SAMPLER2D_AUTOREG(s_SeasonsTexture);
SAMPLER2D_AUTOREG(s_LightMapTexture);

// this code is from Newb X Paretion shader
float getHeightFromTex(vec2 uv, sampler2D tex) {
	vec3 t = texture2D(tex, uv).rgb;
return (t.x+t.y+t.z)/3.0;
}

vec4 getNormalMapFromTex(vec2 uv, vec2 resolution, float scale, sampler2D tex) {
  vec2 step = 1.0 / resolution;

  float height = getHeightFromTex(uv, tex);

  vec2 dxy = height - vec2(
      getHeightFromTex(uv + vec2(step.x, 0.0), tex),
      getHeightFromTex(uv + vec2(0.0, step.y), tex)
  );
  return vec4(normalize(vec3(dxy * scale / step, 1.0)), height);
}

void main() {
// this code is from Newb X Paretion shader
 vec3 nmTex = getNormalMapFromTex(v_texcoord0, vec2(60000.0,60000.0), 1.2, s_MatTexture).xyz;

  #if defined(DEPTH_ONLY_OPAQUE) || defined(DEPTH_ONLY) || defined(INSTANCING)
    gl_FragColor = vec4(1.0,1.0,1.0,1.0);
    return;
  #endif

  vec4 diffuse = texture2D(s_MatTexture, v_texcoord0);
  vec4 color = v_color0;

  #ifdef ALPHA_TEST
    if (diffuse.a < 0.6) {
      discard;
    }
  #endif

  #if defined(SEASONS) && (defined(OPAQUE) || defined(ALPHA_TEST))
    diffuse.rgb *= mix(vec3(1.0,1.0,1.0), texture2D(s_SeasonsTexture, v_color1.xy).rgb * 2.0, v_color1.z);
  #endif

  vec3 glow = nlGlow(s_MatTexture, v_texcoord0, v_extra.a);

  diffuse.rgb *= diffuse.rgb;

  vec3 lightTint = texture2D(s_LightMapTexture, v_lightmapUV).rgb;
  lightTint = mix(lightTint.bbb, lightTint*lightTint, 0.35 + 0.65*v_lightmapUV.y*v_lightmapUV.y*v_lightmapUV.y);

 float lightIntensity = max(dot(nmTex, normalize(vec3(1.0,1.0,0.5))), 0.0);   

    vec3 col = diffuse.rgb;
    diffuse.rgb += diffuse.rgb*lightIntensity;
    diffuse.rgb *= 0.8;
    diffuse.rgb = mix(col, diffuse.rgb, 1.0);

  color.rgb *= lightTint;

  #if defined(TRANSPARENT) && !(defined(SEASONS) || defined(RENDER_AS_BILLBOARDS))
    if (v_extra.b > 0.9) {
      diffuse.rgb = vec3_splat(1.0 - NL_WATER_TEX_OPACITY*(1.0 - diffuse.b*1.8));
      diffuse.a = color.a;
    }
  #else
    diffuse.a = 1.0;
  #endif

  diffuse.rgb *= color.rgb;
  diffuse.rgb += glow;

  if (v_extra.b > 0.9) {
    diffuse.rgb += v_refl.rgb*v_refl.a;
  } else if (v_refl.a > 0.0) {
    // reflective effect - only on xz plane
    float dy = abs(dFdy(v_extra.g));
    if (dy < 0.0002) {
      float mask = v_refl.a*(clamp(v_extra.r*10.0,8.2,8.8)-7.8);
      diffuse.rgb *= 1.0 - 0.6*mask;
      diffuse.rgb += v_refl.rgb*mask;
    }
  }

  diffuse.rgb = mix(diffuse.rgb, v_fog.rgb, v_fog.a);

  diffuse.rgb = colorCorrection(diffuse.rgb);

  gl_FragColor = diffuse;
}

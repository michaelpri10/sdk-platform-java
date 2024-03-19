/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: google/apps/card/v1/card.proto

// Protobuf Java Version: 3.25.2
package com.google.apps.card.v1;

public interface ButtonOrBuilder
    extends
    // @@protoc_insertion_point(interface_extends:google.apps.card.v1.Button)
    com.google.protobuf.MessageOrBuilder {

  /**
   *
   *
   * <pre>
   * The text displayed inside the button.
   * </pre>
   *
   * <code>string text = 1;</code>
   *
   * @return The text.
   */
  java.lang.String getText();
  /**
   *
   *
   * <pre>
   * The text displayed inside the button.
   * </pre>
   *
   * <code>string text = 1;</code>
   *
   * @return The bytes for text.
   */
  com.google.protobuf.ByteString getTextBytes();

  /**
   *
   *
   * <pre>
   * The icon image. If both `icon` and `text` are set, then the icon appears
   * before the text.
   * </pre>
   *
   * <code>.google.apps.card.v1.Icon icon = 2;</code>
   *
   * @return Whether the icon field is set.
   */
  boolean hasIcon();
  /**
   *
   *
   * <pre>
   * The icon image. If both `icon` and `text` are set, then the icon appears
   * before the text.
   * </pre>
   *
   * <code>.google.apps.card.v1.Icon icon = 2;</code>
   *
   * @return The icon.
   */
  com.google.apps.card.v1.Icon getIcon();
  /**
   *
   *
   * <pre>
   * The icon image. If both `icon` and `text` are set, then the icon appears
   * before the text.
   * </pre>
   *
   * <code>.google.apps.card.v1.Icon icon = 2;</code>
   */
  com.google.apps.card.v1.IconOrBuilder getIconOrBuilder();

  /**
   *
   *
   * <pre>
   * If set, the button is filled with a solid background color and the font
   * color changes to maintain contrast with the background color. For example,
   * setting a blue background likely results in white text.
   *
   * If unset, the image background is white and the font color is blue.
   *
   * For red, green, and blue, the value of each field is a `float` number that
   * you can express in either of two ways: as a number between 0 and 255
   * divided by 255 (153/255), or as a value between 0 and 1 (0.6). 0 represents
   * the absence of a color and 1 or 255/255 represent the full presence of that
   * color on the RGB scale.
   *
   * Optionally set `alpha`, which sets a level of transparency using this
   * equation:
   *
   * ```
   * pixel color = alpha * (this color) + (1.0 - alpha) * (background color)
   * ```
   *
   * For `alpha`, a value of `1` corresponds with a solid color, and a value of
   * `0` corresponds with a completely transparent color.
   *
   * For example, the following color represents a half transparent red:
   *
   * ```
   * "color": {
   *    "red": 1,
   *    "green": 0,
   *    "blue": 0,
   *    "alpha": 0.5
   * }
   * ```
   * </pre>
   *
   * <code>.google.type.Color color = 3;</code>
   *
   * @return Whether the color field is set.
   */
  boolean hasColor();
  /**
   *
   *
   * <pre>
   * If set, the button is filled with a solid background color and the font
   * color changes to maintain contrast with the background color. For example,
   * setting a blue background likely results in white text.
   *
   * If unset, the image background is white and the font color is blue.
   *
   * For red, green, and blue, the value of each field is a `float` number that
   * you can express in either of two ways: as a number between 0 and 255
   * divided by 255 (153/255), or as a value between 0 and 1 (0.6). 0 represents
   * the absence of a color and 1 or 255/255 represent the full presence of that
   * color on the RGB scale.
   *
   * Optionally set `alpha`, which sets a level of transparency using this
   * equation:
   *
   * ```
   * pixel color = alpha * (this color) + (1.0 - alpha) * (background color)
   * ```
   *
   * For `alpha`, a value of `1` corresponds with a solid color, and a value of
   * `0` corresponds with a completely transparent color.
   *
   * For example, the following color represents a half transparent red:
   *
   * ```
   * "color": {
   *    "red": 1,
   *    "green": 0,
   *    "blue": 0,
   *    "alpha": 0.5
   * }
   * ```
   * </pre>
   *
   * <code>.google.type.Color color = 3;</code>
   *
   * @return The color.
   */
  com.google.type.Color getColor();
  /**
   *
   *
   * <pre>
   * If set, the button is filled with a solid background color and the font
   * color changes to maintain contrast with the background color. For example,
   * setting a blue background likely results in white text.
   *
   * If unset, the image background is white and the font color is blue.
   *
   * For red, green, and blue, the value of each field is a `float` number that
   * you can express in either of two ways: as a number between 0 and 255
   * divided by 255 (153/255), or as a value between 0 and 1 (0.6). 0 represents
   * the absence of a color and 1 or 255/255 represent the full presence of that
   * color on the RGB scale.
   *
   * Optionally set `alpha`, which sets a level of transparency using this
   * equation:
   *
   * ```
   * pixel color = alpha * (this color) + (1.0 - alpha) * (background color)
   * ```
   *
   * For `alpha`, a value of `1` corresponds with a solid color, and a value of
   * `0` corresponds with a completely transparent color.
   *
   * For example, the following color represents a half transparent red:
   *
   * ```
   * "color": {
   *    "red": 1,
   *    "green": 0,
   *    "blue": 0,
   *    "alpha": 0.5
   * }
   * ```
   * </pre>
   *
   * <code>.google.type.Color color = 3;</code>
   */
  com.google.type.ColorOrBuilder getColorOrBuilder();

  /**
   *
   *
   * <pre>
   * Required. The action to perform when a user clicks the button, such as
   * opening a hyperlink or running a custom function.
   * </pre>
   *
   * <code>.google.apps.card.v1.OnClick on_click = 4;</code>
   *
   * @return Whether the onClick field is set.
   */
  boolean hasOnClick();
  /**
   *
   *
   * <pre>
   * Required. The action to perform when a user clicks the button, such as
   * opening a hyperlink or running a custom function.
   * </pre>
   *
   * <code>.google.apps.card.v1.OnClick on_click = 4;</code>
   *
   * @return The onClick.
   */
  com.google.apps.card.v1.OnClick getOnClick();
  /**
   *
   *
   * <pre>
   * Required. The action to perform when a user clicks the button, such as
   * opening a hyperlink or running a custom function.
   * </pre>
   *
   * <code>.google.apps.card.v1.OnClick on_click = 4;</code>
   */
  com.google.apps.card.v1.OnClickOrBuilder getOnClickOrBuilder();

  /**
   *
   *
   * <pre>
   * If `true`, the button is displayed in an inactive state and doesn't respond
   * to user actions.
   * </pre>
   *
   * <code>bool disabled = 5;</code>
   *
   * @return The disabled.
   */
  boolean getDisabled();

  /**
   *
   *
   * <pre>
   * The alternative text that's used for accessibility.
   *
   * Set descriptive text that lets users know what the button does. For
   * example, if a button opens a hyperlink, you might write: "Opens a new
   * browser tab and navigates to the Google Chat developer documentation at
   * https://developers.google.com/chat".
   * </pre>
   *
   * <code>string alt_text = 6;</code>
   *
   * @return The altText.
   */
  java.lang.String getAltText();
  /**
   *
   *
   * <pre>
   * The alternative text that's used for accessibility.
   *
   * Set descriptive text that lets users know what the button does. For
   * example, if a button opens a hyperlink, you might write: "Opens a new
   * browser tab and navigates to the Google Chat developer documentation at
   * https://developers.google.com/chat".
   * </pre>
   *
   * <code>string alt_text = 6;</code>
   *
   * @return The bytes for altText.
   */
  com.google.protobuf.ByteString getAltTextBytes();
}